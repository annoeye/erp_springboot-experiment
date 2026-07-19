#!/bin/bash
# ============================================================
# ES Removal Verification Test Script
# POST /api/merchandise/search-Product
# Run this directly in your terminal: bash test_script.sh
# ============================================================

BASE_URL="http://localhost:8080"
ENDPOINT="$BASE_URL/api/merchandise/search-Product"
PASS=0
FAIL=0
RESULTS=""

check_response() {
    local test_name="$1"
    local http_code="$2"
    local body="$3"
    local expected_code="${4:-200}"

    # Check for ES error markers
    if echo "$body" | grep -qi "elasticsearch\|9200\|no_shard_available\|DataAccessResourceFailure"; then
        echo "❌ FAIL [$test_name]: ES ERROR in response!"
        echo "   Body: $(echo $body | head -c 300)"
        RESULTS="$RESULTS\n- [ ] $test_name: FAIL (ES error)"
        FAIL=$((FAIL+1))
        return
    fi

    if [ "$http_code" = "$expected_code" ]; then
        echo "✅ PASS [$test_name]: HTTP $http_code"
        RESULTS="$RESULTS\n- [x] $test_name: PASS (HTTP $http_code)"
        PASS=$((PASS+1))
    else
        echo "❌ FAIL [$test_name]: Expected HTTP $expected_code, got $http_code"
        echo "   Body: $(echo $body | head -c 300)"
        RESULTS="$RESULTS\n- [ ] $test_name: FAIL (HTTP $http_code)"
        FAIL=$((FAIL+1))
    fi
}

echo "============================================================"
echo "  ES Removal Verification Test Suite"
echo "  Server: $BASE_URL"
echo "  $(date)"
echo "============================================================"
echo ""

# Step 1: Server health check
echo "=== Step 1: Server Health Check ==="
HEALTH_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health" 2>/dev/null)
if [ "$HEALTH_CODE" = "200" ] || [ "$HEALTH_CODE" = "401" ]; then
    echo "✅ Server is running (HTTP $HEALTH_CODE)"
else
    echo "❌ Server is NOT reachable (HTTP $HEALTH_CODE)"
    echo "   Cannot continue. Is the Spring Boot app running on port 8080?"
    exit 1
fi
echo ""

# Step 2: ES port check (should be unreachable)
echo "=== Step 2: Elasticsearch Port Check ==="
ES_CODE=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 2 "http://localhost:9200" 2>/dev/null)
if [ "$ES_CODE" = "200" ] || [ "$ES_CODE" = "000" ] || [ -z "$ES_CODE" ]; then
    if [ "$ES_CODE" = "200" ]; then
        echo "⚠️  WARNING: Elasticsearch IS running on port 9200"
    else
        echo "✅ Elasticsearch is NOT running on port 9200 (as expected)"
    fi
else
    echo "✅ Elasticsearch is NOT accessible on port 9200 (HTTP $ES_CODE)"
fi
echo ""

# Step 3: Run all search tests
echo "=== Step 3: Search API Tests ==="
echo ""

# Test 1: Empty body
echo "--- Test 1: Empty body {} ---"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$ENDPOINT" \
    -H "Content-Type: application/json" \
    -d '{}')
HTTP_CODE=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
check_response "T1 Empty body {}" "$HTTP_CODE" "$BODY"
echo "   totalElements: $(echo $BODY | grep -o '"totalElements":[0-9]*' | head -1)"
echo ""

# Test 2: Keyword filter
echo "--- Test 2: keyword='a' ---"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$ENDPOINT" \
    -H "Content-Type: application/json" \
    -d '{"keyword": "a"}')
HTTP_CODE=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
check_response "T2 keyword filter" "$HTTP_CODE" "$BODY"
echo "   totalElements: $(echo $BODY | grep -o '"totalElements":[0-9]*' | head -1)"
echo ""

# Test 3: Status ACTIVE
echo "--- Test 3: statuses=['ACTIVE'] ---"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$ENDPOINT" \
    -H "Content-Type: application/json" \
    -d '{"statuses": ["ACTIVE"]}')
HTTP_CODE=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
check_response "T3 status ACTIVE" "$HTTP_CODE" "$BODY"
# Check that all returned items have status ACTIVE (if any)
LOCKED_COUNT=$(echo "$BODY" | grep -o '"status":"LOCKED"' | wc -l)
if [ "$LOCKED_COUNT" -gt "0" ]; then
    echo "   ❌ Found LOCKED items in ACTIVE-only results!"
else
    echo "   ✅ No LOCKED items in ACTIVE results"
fi
echo ""

# Test 4: Status LOCKED
echo "--- Test 4: statuses=['LOCKED'] ---"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$ENDPOINT" \
    -H "Content-Type: application/json" \
    -d '{"statuses": ["LOCKED"]}')
HTTP_CODE=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
check_response "T4 status LOCKED" "$HTTP_CODE" "$BODY"
echo ""

# Test 5: Pagination size=5
echo "--- Test 5: paging page=1, size=5 ---"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$ENDPOINT" \
    -H "Content-Type: application/json" \
    -d '{"paging": {"page": 1, "size": 5}}')
HTTP_CODE=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
check_response "T5 paging size=5" "$HTTP_CODE" "$BODY"
SIZE_VAL=$(echo "$BODY" | grep -o '"size":[0-9]*' | head -1)
echo "   $SIZE_VAL (should be <=5)"
echo ""

# Test 6: minSoldQuantity
echo "--- Test 6: minSoldQuantity=0 ---"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$ENDPOINT" \
    -H "Content-Type: application/json" \
    -d '{"minSoldQuantity": 0}')
HTTP_CODE=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
check_response "T6 minSoldQuantity" "$HTTP_CODE" "$BODY"
echo ""

# Test 7: minRevenue
echo "--- Test 7: minRevenue=0.0 ---"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$ENDPOINT" \
    -H "Content-Type: application/json" \
    -d '{"minRevenue": 0.0}')
HTTP_CODE=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
check_response "T7 minRevenue" "$HTTP_CODE" "$BODY"
echo ""

# Test 8: Date range
echo "--- Test 8: createdFrom/createdTo ---"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$ENDPOINT" \
    -H "Content-Type: application/json" \
    -d '{"createdFrom": "2020-01-01T00:00:00", "createdTo": "2030-12-31T23:59:59"}')
HTTP_CODE=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
check_response "T8 date range" "$HTTP_CODE" "$BODY"
echo ""

# Test 9: Combined filters
echo "--- Test 9: combined keyword+status+paging ---"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$ENDPOINT" \
    -H "Content-Type: application/json" \
    -d '{"keyword": "a", "statuses": ["ACTIVE"], "paging": {"page": 1, "size": 3}}')
HTTP_CODE=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
check_response "T9 combined filters" "$HTTP_CODE" "$BODY"
echo ""

# Test 10: maxSoldQuantity
echo "--- Test 10: maxSoldQuantity filter ---"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$ENDPOINT" \
    -H "Content-Type: application/json" \
    -d '{"maxSoldQuantity": 1000}')
HTTP_CODE=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
check_response "T10 maxSoldQuantity" "$HTTP_CODE" "$BODY"
echo ""

# Check app logs for ES errors
echo "=== Step 4: Log Check for ES Errors ==="
LOG_DIR="/home/ddicgegd/Projects/erp_springboot-experiment/logs"
if ls "$LOG_DIR"/*.log 2>/dev/null | head -1 > /dev/null; then
    ES_LOG_HITS=$(tail -200 "$LOG_DIR"/*.log 2>/dev/null | grep -ci 'elasticsearch\|9200\|shard_available' || echo 0)
    if [ "$ES_LOG_HITS" -eq "0" ]; then
        echo "✅ No ES-related log entries found in recent logs"
    else
        echo "⚠️  Found $ES_LOG_HITS ES-related log entries"
        tail -200 "$LOG_DIR"/*.log 2>/dev/null | grep -i 'elasticsearch\|9200\|shard_available' | head -5
    fi
else
    echo "ℹ️  No log files found at $LOG_DIR"
fi
echo ""

# Summary
echo "============================================================"
echo "  FINAL RESULTS"
echo "============================================================"
echo -e "$RESULTS"
echo ""
echo "  PASS: $PASS / $((PASS+FAIL))"
echo "  FAIL: $FAIL / $((PASS+FAIL))"
echo ""
if [ "$FAIL" -eq "0" ]; then
    echo "  🎉 ALL TESTS PASSED — ES removal verified!"
else
    echo "  ❌ SOME TESTS FAILED — Review above output"
fi
echo "============================================================"
