# 🧪 Test Suite - Pompilius Backend

This directory contains the automated test suite for the Pompilius backend using **ScalaTest** and **Mockito**.

## 📁 Test Structure

```
test/
└── dev/pompilius/
    ├── attachment/infrastructure/repositories/
    │   └── AttachmentMySqlRepositorySpec.scala      # Repository behavior tests
    └── resource/application/
        └── ResourceServiceSpec.scala                # Service tests with Mockito mocks
```

## 🎯 Test Examples

### 1. **AttachmentMySqlRepositorySpec**
Tests the repository layer without mocks (specification tests):
- Documents expected SQL query behavior
- Tests the `countByType` logic (images vs files)
- Covers all repository methods

### 2. **ResourceServiceSpec** 
Demonstrates **Mockito** usage for unit testing:
- **Mocking dependencies** - Mock the `AttachmentRepository`
- **Stubbing methods** - Configure mock responses with `when(...).thenReturn(...)`
- **Verifying interactions** - Check method calls with `verify(...)`
- **Testing Futures** - Handle async operations
- **Testing exceptions** - Verify error handling

## 🚀 Running Tests

### Run all tests
```bash
sbt test
```

### Run specific test file
```bash
sbt "testOnly dev.pompilius.attachment.infrastructure.repositories.AttachmentMySqlRepositorySpec"
sbt "testOnly dev.pompilius.resource.application.ResourceServiceSpec"
```

### Run tests matching a pattern
```bash
sbt "testOnly *AttachmentSpec"
sbt "testOnly *ResourceServiceSpec"
```

### Run with coverage
```bash
sbt clean coverage test coverageReport
```

Coverage reports will be generated in: `target/scala-2.13/scoverage-report/`

### Run specific test case
```bash
sbt 'testOnly *ResourceServiceSpec -- -z "count images correctly"'
```

## 📚 Mockito Usage Examples

### 1. Basic Mocking
```scala
val mockRepo = mock[AttachmentRepository]
```

### 2. Stubbing Method Returns
```scala
when(mockRepo.countByType(any[ResourceId], eq("image")))
  .thenReturn(Future.successful(5))
```

### 3. Verifying Method Calls
```scala
verify(mockRepo).countByType(resourceId, "image")
verify(mockRepo, times(2)).findById(any[AttachmentId])
verify(mockRepo, never()).delete(any[AttachmentId])
```

### 4. Argument Matchers
```scala
any[ResourceId]        // Matches any ResourceId
eq("image")            // Matches exactly "image"
anyString()            // Matches any String
```

### 5. Testing Exceptions
```scala
when(mockRepo.findById(any[AttachmentId]))
  .thenReturn(Future.failed(new RuntimeException("Error")))
```

## 🔍 Key Testing Patterns

### Testing Async Operations (Futures)
```scala
val result: Future[Int] = mockRepo.countByType(resourceId, "image")

whenReady(result) { count =>
  count shouldBe 5
}
```

### Testing Optional Results
```scala
whenReady(result) { attachmentOpt =>
  attachmentOpt shouldBe defined
  attachmentOpt.get.filename shouldBe "test.jpg"
}
```

### Testing Collections
```scala
whenReady(result) { attachments =>
  attachments should have size 3
  attachments.head.filename shouldBe "image1.jpg"
}
```

## 🎓 Test Coverage Goals

The test suite covers:

✅ **Repository Layer**
- CRUD operations
- Filtering and querying
- Soft deletes
- Preview image management

✅ **Service Layer** (with mocks)
- Business logic
- Error handling
- Dependency interactions
- Async operations

## 🛠️ Test Dependencies

Configured in `build.sbt`:

```scala
"org.scalatestplus.play" %% "scalatestplus-play" % "7.0.2" % Test
"org.scalatestplus" %% "mockito-3-4" % "3.2.10.0" % Test
"org.mockito" % "mockito-core" % "5.18.0" % Test
```

## 📖 Additional Resources

- [ScalaTest Documentation](http://www.scalatest.org/)
- [Mockito Scala](https://github.com/mockito/mockito-scala)
- [Play Framework Testing](https://www.playframework.com/documentation/latest/ScalaTestingWithScalaTest)

## 🎯 Best Practices

1. **Use descriptive test names** - Tests should read like specifications
2. **Follow AAA pattern** - Arrange, Act, Assert
3. **Mock external dependencies** - Keep tests isolated and fast
4. **Test edge cases** - Not just happy paths
5. **Keep tests independent** - Each test should run in isolation
6. **Use appropriate matchers** - Make assertions clear and readable

## 🐛 Debugging Tests

### Run tests with detailed output
```bash
sbt "testOnly *ResourceServiceSpec -- -oD"
```

### Run tests in a specific timezone
```bash
sbt -Duser.timezone=GMT test
```

### Run with custom test configuration
```bash
sbt -Dtest.config.resource=application-test.conf test
```

---

**Happy Testing! 🧪**

