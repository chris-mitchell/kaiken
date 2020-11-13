# Kaiken

Kaiken is a library and and annotation processor to faciliate management of dependencies that supports multi-app, multi-user and user optional scenarios.

### The problems:
+ It's hard for Activities/Fragments to "locate" dependencies for modules that are re-used by different applications.
+ It's hard to support multiple users without falling back to have a single "active" user.
+ It's hard to resolve dependencies for modules that can run both in an "authenticated mode" and "non-authenticated mode".

Kaiken is an opinionated library for user scoping and location of dependencies for companies that needs to share modules across multiple applications, that need to support multiple users logged-in at the same time and optionally that requires some of this features to be able to access both by an authenticated user as well as when the user is not authenticated.

Kaiken is composed of two libraries that can be used independetely of each other: a depedency location library and an annotation processors.


### Kaiken guiding principles

**Support modules that be can be used by different Android apps**
Every app should be able to declare a different set of user dependences. The implication of this is that there cannot be a single “UserServices” interface that the feature modules can rely on.  

**Allow for a gradual migration**
Allow adoptions feature by feature, instead of requiring a massive rearchitecture of the app.

**Support for multiple concurrent users (N users)**
We need to allow multiple users to be logged in at the same time. While only one user will be an “active user” for UI purposes, we need to allow for other user's services to run on the background. 

**Support for modules that work in both Auth and NoAuth scenarios**
The app could support features that can work in both authenticated and not authenticated scenarios (e.g. file previewing).

**Allow location of dependencies by top level UI components**
- Location of app/global services and user scoped level services. 
- Provide access to those services from Activities and Fragments.

**Isolation of user services**
We need to make sure that there's no possibility of code running under the wrong user. We need to maintain the current behavior of tearing down all user services as well as related UI when a user is logged out.

**Allow explicit teardown of services**
We need to allow user services to expose “teardown” code. Code should be guaranteed to be called before the service is removed from memory. This allows for closing of streams and flushing of caches and queues.

**Compatible with open source UI Frameworks**

**Do not marry the framework**
While we need to ensure compatibility with Dagger, given that it is the standard for Android Injection in Android. The common interfaces shouldn't be tied to it. It is OK to depend on the `@Inject` annotation (which is part of the Java Spec), but not on Dagger specific behavior.

**Composition over inheritance**
We will aim for a composition implementation that avoid base classes in favor of base interfaces.

### Kaiken non-goals
+ Dictate how initialization of Application Level Services should be performed
+ Provide access to user dependencies below the fragment level
While we will allow the immediate fragment children of activities to locate user dependencies. Location of dependencies dependencies below the fragment is not solved (i.e. views).

+ Differentiation between background and foreground user services
There will be single unified user services. While it is up to the implementing app to define how the users services are initialized. So it can lazy initialize some of the services. We won't support the semantics of foreground vs background services.


### Kaiken Scoping

Let's say that you have an active that you know always has to be run under user scoped (i.e. a user most be logged in to see that fragment). The activity requires two dependencies, an app scoped AppServiceFoo (e.g. network manager) and a user scoped UserServiceBar (e.g. user profile manager):

```
class MyActivity : AppCompatActivity, AuthRequiredActivity {

    private lateinit var appServiceFoo: AppServiceFoo
    private lateinit var userServiceBar: UserServiceBar

    @Override
    fun onCreate() {
       if (finishIfInvalidAut()) {
           return
       }

       val dependencies: MyActivityDependencies = resolveDependencyProvider()

       appServiceFoo = dependencies.appServiceFoo
       userServiceBar = dependencies.userServiceBar

       // You succesfully located your dependencies, you can do what ever you want with them!
    }
}
```

You'll notice that we're referencing a `MyActivityDependencies` type. This is an interface that you should define for your "feature". In a world were we could expose constructors in Activities and Fragments, these would be parameters that those constructors would take. The feature doesn't really care were those dependencies come from, as long as they're satisfied. Going back to the example, the interface would be defines like this:

```
interface MyActivityDependencies {
    val appServicesFoo: AppServicesFoo
    val userServicesBar: UserServicesBar
}
```

Before we go into the details of how `resolveDependencyProvider` works exactly. Let's finish see how the dependency provision looks on the side of the app.

```
class MyUserServices : MyActivityDependencies {

}
```

Unfortunately there's really not a good way to avoid having our user services class implement all of the feature interfaces. However if you use Dagger and Anvil, which we recommend, you can avoid having to list all of the interfaces by creating two Anvil scopes: one for users and one for app services.

### Kaiken Processor

While the Kaiken Scoping by itself allows you to retieve user and app scoped services, it doesn't allow you to create feature level scopes. To solve that problem we introduce an annotation processor.

Going back to our example:

```
import com.dropbox.kaiken.annotations.Injectable

@Injectable
class MyActivity : AppCompatActivity, AuthRequiredActivity, InjectorHolder<MyActivityInjector> {

    @Injectable lateinit var appServiceFoo: AppServiceFoo
    @Injectable lateinit var userServiceBar: UserServiceBar
    @Injectable lateinit var featureServiceBaz: featuerServiceBaz

    @Override
    fun onCreate() {
       if (finishIfInvalidAuth()) {
           return
       }

       inject()

       // You succesfully located your dependencies, you can do what ever you want with them!
    }
}
```

The `@Injectable` interface generates two files:

1) An "Injector" interface for your activity or fragments:

```
interface MyActivityInjector {
    void inject(MyActivity myActivity);
}
```

2) A Kotlin `inject` extension function:

```
fun MyActivity.inject() {
  val injector: MyActivityInjector = findInjector()
  injector.inject(this)
}
```

This allows you to define an injector object for your app that will be retrained across configuration chnages and that is scoped to the lifecycle of the Activity/Fragment.

For example, if you were using Dagger, you could define a components such as this:

```
@Component(dependencies=[MyActivityDependencies:class])
class MyActivityComponent : MyActivityInjector {
    @Component.Factory
    interface Factory {
        fun create(
            myActivityDependencies: MyActivityDependencies
        ): MyActivityComponent
    }    
}
```

### Why we decided not to use Hilt?
Unfortunately adding custom scopes in Hilt require to replicate the entire tree. Plus there's really not a good way of exposing user services for different applications. There user services of application A can be different that the user services of Application B. So passing a "UserManager" (that contains all possible user services) as a single dependency was not an option for us.
