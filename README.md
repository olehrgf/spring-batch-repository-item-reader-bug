# spring-batch-repository-item-reader-bug

Simple example for reproducing of `RepositoryItemReader` bug in spring batch.

## Description of a bug

1. Read, process and commit first N chanks(pages)
2. Throw some exception in N+1 chank -> commit failed
3. Restart job -> reader will execute `jumpToPage(N)` method and load last processed element

### Expected behaviour 
Reader should load N+1 page and first not processed element
