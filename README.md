# Slim Repo [![Build Status](https://travis-ci.org/slim-gears/slimrepo.svg?branch=master)](https://travis-ci.org/slim-gears/slimrepo) [![Maven Repository](https://img.shields.io/github/tag/slim-gears/slimrepo.svg?label=maven)](https://jitpack.io/#slim-gears/slimrepo)
### Light-weight modular ORM for Java and Android

##### The library is still under development. Stay tuned for updates.

Background
---

The library was inspired by [GreenDAO](http://greendao-orm.com/ "GreenDAO") and [Microsoft Entity Framework Code First](https://msdn.microsoft.com/en-us/data/ee712907) 

#### Terminology

`Entity` - Data object, POJO 
 
`Repository` - represents abstract working session, *unit-of-work* against ORM

`RepositoryService` - factory, allowing to create `Repository` instances 

#### Features

* **Modularity** - same Front-End (`Repository` and `Entities`), being able working transparently with different Back-Ends (underlying persistent storages - e.g. Sqlite, document db, remote RESTful service, etc.)
* **Annotation processing based** - no reflection usage in run-time, *proguard-friendly*
* **Intuitive syntax** - intuitive, type-safe and highly readable syntax
* **Bulk operations support** *Bulk update* and *bulk delete* are supported
* **Light-weight**

---

Repository Definition
---

##### Entity definition
**Step 1:** Define one or more entities:
```java
@GenerateEntity
public class AbstractUserEntity {
    @Key protected int userId;
    protected String firstName;
    protected String lastName;
    protected Date lastVisitDate;
}
```
Real entity will be named `UserEntity`, and will be generated by annotation processor.

##### Repository definition
**Step 2:** Define one or more repositories. It's just an interface containing one or more `EntitySets`
```java
@GenerateRepository
public interface UserRepository extends Repository {
    EntitySet<Integer, UserEntity> users();
    EntitySet<Integer, CountryEntity> countries();
    EntitySet<Integer, OrderEntity> orders();
}
```
Insert
---

First, create `RepositoryService<UserRepository>` instance by instantiating generated `UserRepositoryService`

```java
RepositoryService<UserRepository> repoService = new UserRepositoryService(context);
```

**Option 1** - create repository instance using `RepositoryService.open()`: 

```java
try (UserRepository repo = repoService.open()) {
	EntitySet<UserEntity> users = repo.users();

	// Possible syntax
	users.addNew()
		.setFirstName("John")
		.setLastName("Doe")
		.setLastVisitDate(Dates.now());

	// Alternative syntax
	users.add(UserEntity.create()
		.firstName("William")
		.lastName("Shakespeare")
		.lastVisitDate(Dates.now())
		.build());

	// Explicitly saving changes
	repo.saveChanges();
}
```

**Alternative *(and preferred)* option** for updating is to use `RepositoryService.update()` method.
In this case changes will be saved automatically:

```java
repoService.update(repo -> {
	EntitySet<UserEntity> users = repo.users();

	// Possible syntax
	users.addNew()
		.setFirstName("John")
		.setLastName("Doe")
		.setLastVisitDate(Dates.now());

	// Alternative syntax
	users.add(UserEntity.create()
		.firstName("William")
		.lastName("Shakespeare")
		.lastVisitDate(Dates.now())
		.build());
});
```

Query
---

**Option 1** - using `RepositoryService.open()`: 

```java
try (UserRepository repo = repoService.open()) {
	long count = repo.users().query()
		.where(UserEntity.LastVisitDate.between(Dates.yesterday(), Dates.today())
		.prepare()
		.count();
}
```

**Option 2** - using `RepositoryService.query()`: 

```java 
UserEntity[] repoService.query(repo -> {
	return repo.users().query()
		.where(UserEntity.UserFirstName.contains("a"))
		.prepare()
		.toArray();
});
```

**Using and / or condition compositions**

```java
UserEntity[] repoService.query(repo -> {
	return repo.users().query()
		.where(Conditions
			.and(
				UserEntity.FirstName.contains("a"),
				UserEntity.LastName.endsWith("e"))
			.or(UserEntity.LastVisitDate.greaterThan(Dates.today()))
		.prepare()
		.toArray();
});
```

Update
---

**Single entity update**: 

```java
repoService.update(repo -> {
	UserEntity user = repo.users().findById(2);
	user.setLastName("Smith");
});
```

**Bulk update**: Updating all entities, matching `where` criteria:

```java 
repoService.update(repo -> {
	return repo.users().update()
		.where(UserEntity.UserFirstName.contains("a"))
		.set(UserEntity.LastVisitDate, Dates.now())
		.prepare()
		.execute();
});
```

Delete
---

**Delete single entity directly**

```java
repoService.update(repo -> {
	UserEntity user = repo.users().findById(2);
	repo.users().remove(user);
});
```

**Bulk delete**: Deleting all entities, matching `where` criteria

```java
repoService.update(repo -> {
	return repo.users().delete()
		.where(UserEntity.UserFirstName.contains("a"))
		.prepare()
		.execute();
});
```
