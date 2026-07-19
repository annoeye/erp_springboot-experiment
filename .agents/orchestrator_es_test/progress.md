# Orchestrator ES Test Progress

## Current Status
Last updated: 2026-07-15T02:50:00+07:00

- [x] Explore codebase: ProductService uses JPA Specification (not ES)
- [x] Check pom.xml: ES dependency present but ProductService confirmed no ES usage
- [x] Server health check: localhost:8080 is UP (HTTP 200)
- [x] All acceptance criteria tests executed
- [x] Results compiled

## Phase: COMPLETE

## Key Findings
- ProductService.searchProducts() uses SpecificationBuilder + JPA (buildProductSpecification)
- Comment in code: "Tìm kiếm hiện dùng JPA Specification trực tiếp từ DB"
- /api/merchandise/** is permitAll() - no JWT needed for search
- PagingRequest.page is 1-based (pageable() does page-1 internally)
- createdFrom/createdTo requires ISO LocalDateTime format (e.g. 2020-01-01T00:00:00), not date-only
- minSoldQuantity uses GREATER_THAN (strict), so 0 returns only products with >0 soldQty — correct behavior
- Elasticsearch is NOT running at localhost:9200 — API works fine without it

## Test Results

| Test | Criteria | HTTP | Result | PASS? |
|------|----------|------|--------|-------|
| 1 | Empty body {} | 200 | 5 products, content=list | ✅ |
| 2 | keyword="a" | 200 | 4 products (all have 'a' in name) | ✅ |
| 3 | statuses=["ACTIVE"] | 200 | 5 products all ACTIVE | ✅ |
| 4 | page=1,size=5 | 200 | totalElements=5, size=5 | ✅ |
| 5 | statuses=["LOCKED"] | 200 | 0 products (no LOCKED data) | ✅ |
| 6 | keyword="iphone" | 200 | ["iPhone 15 Pro Max"] only | ✅ |
| 7 | ES down (9200 unreachable) | - | API still returns 200 | ✅ |
| 8 | minSoldQuantity=0 | 200 | 0 results (GREATER_THAN, products have qty=0) — correct | ✅ |
| 9 | createdFrom/createdTo (ISO) | 200 | 5 products returned | ✅ |
| 10 | keyword+statuses combined | 200 | 3 matching products | ✅ |
| 11 | No ES trace in response body | - | CLEAN — no elasticsearch/9200 in response | ✅ |

## All Acceptance Criteria: PASSED ✅
