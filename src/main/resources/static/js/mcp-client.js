/**
 * MCP Client for Browser (JavaScript)
 * 
 * Kết nối Frontend tới MCP Server qua Streamable HTTP transport.
 * Sử dụng SSE (Server-Sent Events) để nhận notifications từ server.
 * 
 * Usage:
 *   const mcp = new McpClient('/mcp');
 *   await mcp.initialize();
 *   const tools = await mcp.listTools();
 *   const result = await mcp.callTool('kafka_topics', {});
 */
class McpClient {
    constructor(serverUrl) {
        this.serverUrl = serverUrl;
        this.sessionId = null;
        this.requestId = 0;
        this.pendingRequests = new Map();
        this.eventSource = null;
        this.initialized = false;
        this.eventListeners = {};
    }

    /**
     * Khởi tạo kết nối MCP
     */
    async initialize() {
        // 1. Mở SSE stream trước
        await this._openEventStream();

        // 2. Gửi Initialize Request
        const response = await this._request('initialize', {
            protocolVersion: '2025-06-18',
            capabilities: {
                sampling: {},
                elicitation: {}
            },
            clientInfo: {
                name: 'erp-frontend',
                version: '1.0.0'
            }
        });

        if (response && response.result) {
            this.initialized = true;
            
            // 3. Gửi initialized notification
            await this._notify('notifications/initialized');
            
            console.log('[MCP] Connected:', response.result.serverInfo);
            this._dispatchEvent('connected', response.result);
        }

        return response;
    }

    /**
     * Lấy danh sách tools từ tất cả MCP servers
     */
    async listTools() {
        const response = await this._request('tools/list');
        return response?.result?.tools || [];
    }

    /**
     * Gọi một tool
     */
    async callTool(name, args = {}) {
        const response = await this._request('tools/call', {
            name,
            arguments: args
        });
        return response?.result;
    }

    /**
     * Lấy danh sách resources
     */
    async listResources() {
        const response = await this._request('resources/list');
        return response?.result?.resources || [];
    }

    /**
     * Đọc resource
     */
    async readResource(uri) {
        const response = await this._request('resources/read', { uri });
        return response?.result;
    }

    /**
     * Ping MCP server
     */
    async ping() {
        const response = await this._request('ping');
        return response?.result;
    }

    /**
     * Đóng kết nối
     */
    disconnect() {
        if (this.eventSource) {
            this.eventSource.close();
            this.eventSource = null;
        }
        this.initialized = false;
        this.sessionId = null;
        
        // DELETE session
        if (this.sessionId) {
            fetch(this.serverUrl, {
                method: 'DELETE',
                headers: { 'Mcp-Session-Id': this.sessionId }
            }).catch(() => {});
        }
        
        this._dispatchEvent('disconnected');
    }

    // === Event Listeners ===

    on(event, callback) {
        if (!this.eventListeners[event]) {
            this.eventListeners[event] = [];
        }
        this.eventListeners[event].push(callback);
        return this;
    }

    off(event, callback) {
        if (!this.eventListeners[event]) return;
        this.eventListeners[event] = this.eventListeners[event]
            .filter(cb => cb !== callback);
        return this;
    }

    // === Convenience methods for ERP systems ===

    /**
     * Gửi message vào Kafka topic
     */
    async kafkaProduce(topic, value, key = null) {
        return this.callTool('kafka_produce', { topic, value, ...(key ? { key } : {}) });
    }

    /**
     * Đọc message từ Kafka topic
     */
    async kafkaConsume(topic, count = 10) {
        return this.callTool('kafka_consume', { topic, count });
    }

    /**
     * Danh sách Kafka topics
     */
    async kafkaTopics() {
        return this.callTool('kafka_topics');
    }

    /**
     * Đọc giá trị từ Redis
     */
    async redisGet(key) {
        return this.callTool('redis_get', { key });
    }

    /**
     * Ghi giá trị vào Redis
     */
    async redisSet(key, value, ttlSeconds = null) {
        const args = { key, value };
        if (ttlSeconds) args.ttl_seconds = ttlSeconds;
        return this.callTool('redis_set', args);
    }

    /**
     * Tìm keys trong Redis
     */
    async redisKeys(pattern) {
        return this.callTool('redis_keys', { pattern });
    }

    /**
     * Query database
     */
    async dbQuery(sql, limit = 50) {
        return this.callTool('db_query', { sql, limit });
    }

    /**
     * Danh sách tables
     */
    async dbTables() {
        return this.callTool('db_tables');
    }

    /**
     * Schema của table
     */
    async dbTableSchema(table) {
        return this.callTool('db_table_schema', { table });
    }

    /**
     * Danh sách MinIO buckets
     */
    async minioBuckets() {
        return this.callTool('minio_buckets');
    }

    /**
     * Danh sách objects trong bucket
     */
    async minioList(bucket, prefix = '') {
        return this.callTool('minio_list', { bucket, ...(prefix ? { prefix } : {}) });
    }

    // === Internal Methods ===

    async _request(method, params = null) {
        const id = ++this.requestId;
        const request = {
            jsonrpc: '2.0',
            id,
            method,
            ...(params ? { params } : {})
        };

        return new Promise((resolve, reject) => {
            const timeout = setTimeout(() => {
                this.pendingRequests.delete(id);
                reject(new Error(`Request timeout: ${method}`));
            }, 30000);

            this.pendingRequests.set(id, { resolve, reject, timeout });

            const headers = {
                'Content-Type': 'application/json',
                'Accept': 'application/json, text/event-stream'
            };
            if (this.sessionId) {
                headers['Mcp-Session-Id'] = this.sessionId;
            }

            fetch(this.serverUrl, {
                method: 'POST',
                headers,
                body: JSON.stringify(request)
            })
            .then(async response => {
                const data = await response.text();
                
                // Lưu session ID từ response
                const sid = response.headers.get('Mcp-Session-Id');
                if (sid) {
                    this.sessionId = sid;
                }
                
                // Parse JSON response
                try {
                    const json = JSON.parse(data);
                    this._handleResponse(json);
                } catch (e) {
                    // Có thể là SSE stream
                    console.warn('[MCP] Non-JSON response:', data.substring(0, 100));
                }
            })
            .catch(error => {
                const pending = this.pendingRequests.get(id);
                if (pending) {
                    clearTimeout(pending.timeout);
                    this.pendingRequests.delete(id);
                    pending.reject(error);
                }
            });
        });
    }

    async _notify(method, params = null) {
        const notification = {
            jsonrpc: '2.0',
            method,
            ...(params ? { params } : {})
        };

        const headers = { 'Content-Type': 'application/json' };
        if (this.sessionId) {
            headers['Mcp-Session-Id'] = this.sessionId;
        }

        try {
            await fetch(this.serverUrl, {
                method: 'POST',
                headers,
                body: JSON.stringify(notification)
            });
        } catch (e) {
            console.warn('[MCP] Notification failed:', method, e);
        }
    }

    _handleResponse(message) {
        if (message.id && this.pendingRequests.has(message.id)) {
            const pending = this.pendingRequests.get(message.id);
            clearTimeout(pending.timeout);
            this.pendingRequests.delete(message.id);
            
            if (message.error) {
                pending.reject(new Error(`[MCP Error ${message.error.code}] ${message.error.message}`));
            } else {
                pending.resolve(message);
            }
        }
    }

    _openEventStream() {
        return new Promise((resolve) => {
            // Tạm thời dùng polling vì EventSource không hỗ trợ headers
            // Trong production, nên dùng fetch + ReadableStream
            this._pollServerMessages();
            resolve();
        });
    }

    _pollServerMessages() {
        // Poll server for messages (thay thế cho SSE khi không có session)
        setInterval(() => {
            if (this.sessionId && this.initialized) {
                // Lightweight ping để giữ session alive
                this.ping().catch(() => {});
            }
        }, 30000);
    }

    _dispatchEvent(event, data) {
        if (this.eventListeners[event]) {
            this.eventListeners[event].forEach(cb => cb(data));
        }
    }
}

// Export cho browser
if (typeof window !== 'undefined') {
    window.McpClient = McpClient;
}
