# Coroutines
[TOC]

## Basic
### Definition
#### 线程和协程的区别(Blocking VS Suspending)
> 协程是通过**编译技术**来实现的（不需要虚拟机VM/操作系统OS的支持），通过**插入相关代码**来生效

> 与之相反，线程/进程是需要虚拟机VM/操作系统OS的支持，通过调度CPU执行生效

#### 线程
说到线程，有一个关键概念是阻塞，Thread.sleep是一个典型的阻塞方法
1. 阻塞的代价昂贵，尤其在高负载时的可用线程很少
2. 阻塞线程会导致一些重要任务因为缺少可用线程而被延迟

#### 协程
1. 协程像是轻量级的线程，但并不完全是线程
2. 协程可以让你顺序地写异步代码，极大地降低了异步编程带来的负担
3. 协程更加高效。多个协程可以共用一个线程。一个 App 可以运行的线程数是有限的，但是可以运行的协程数量几乎是无限的

协程是一种叫做`suspending function`
1. 挂起几乎无代价，无需上下文切换或涉及OS
2. 协程挂起可由用户控制：可决定挂起时发生什么，并根据需求优化/记录日志/拦截

> 协程并不是为了取代线程，协程对线程进行抽象，你可以看成协程是一个异步调用的框架，解决了之前线程间协作代码繁琐的问题。协程的实现底层确实也有通过线程来做的（并不是全部），所以我们需要将线程和协程的概念区分开来，协程 != 线程

### Coroutines Concept

#### Suspending Functions
- `launch`
    launch函数是最常用的协程构建器，它不阻塞当前线程，而是在后台创建一个新协程。也可以对其指定协程调度器。
- `delay`
    delay与线程的sleep有异曲同工之处：都是休息一段时间。不过两者还是有点区别的，sleep是线程进入了等待环节，而delay则是执行流进入了等待环节，因此delay不会像sleep一样阻塞其他线程的执行, 但只能在协程中被调用，用于暂停协程
- `suspend`
    以特殊修饰符suspend修饰的函数被称为挂起函数,实现不阻塞。挂起函数只能在协程中和其他挂起函数中调用，不能在其他部分使用，所以要启动一个协程，挂起函数是必须的

#### Coroutines Builder
- `launch`
    最简单常用的协程构造方式，不会阻塞当前线程
- `runBlocking`
    这个协程构造器会阻塞当前线程，直到协程内的所有任务执行完毕。
- `async` 
    - 如`launch`，async也是创建协程并立即启动，但是`launch`是带协程执行结果的启动方式,其返回值类型是`Job`，无法携带返回值
    - 而`async`的返回值类型是`Deferred`，是`Job`的子类，通过调用`Deferred`里的`await`函数，可以得到协程的返回值,关于`Deferred`后续会详细说明

#### Structured Concurrency
协程可以在执行操作所在的指定作用域内启动，而不想线程(线程总是全局的)那样在GlobalScope启动. 这就是Coroutines的结构化并发
> 当我们使用 GlobalScope.launch 时，我们会创建一个顶层协程。虽然它很轻量，但它运行时仍会消耗一些内存资源。如果我们忘记保持对新启动的协程的引用，它还会继续运行。如果协程中的代码挂起了会怎么样（例如，我们错误地延迟了太长时间），如果我们启动了太多的协程并导致内存不足会怎么样？ 必须手动保持对所有已启动协程的引用并 join 之很容易出错。

#### Coroutines Scope
不同的协程构建器构建协程的时候都会提供协程作用域，也可以使用 coroutineScope 构建器声明自己的作用域。它会创建一个协程作用域并且在所有已启动子协程执行完毕之前不会结束。`runBlocking` 与 `coroutineScope` 的主要区别在于后者在等待所有子协程执行完毕时不会阻塞当前线程。
#### Extract Function Refracting
我们来将`launch { …… }`内部的代码块提取到独立的函数中。当你对这段代码执行“提取函数”重构时，你会得到一个带有`suspend`修饰符的新函数。在协程内部可以像普通函数一样使用挂起函数，同样可以使用其他挂起函数如`delay`）来挂起协程的执行。
> `suspend`表明该函数为协程函数，只能运行在协程中
> 协程中可以执行普通函数，但是普通函数不允许执行如`delay`等协程操作

#### Others
- 协程是轻量的，可以同时构建很多个协程(开100_000个协程，每个协程打印一个点)
- 线程结束协程即结束，协程并不会使进程保活

## Cancellation and Timeouts
## Channels
## Composing Suspending Functions
## Coroutines Context and Dispatchers
## Exception Handling

# REFERENCE
[Coroutines Guide From Kotlin](https://kotlinlang.org/docs/reference/coroutines/coroutines-guide.html)

[kotlinx.coroutines-cn](https://github.com/hltj/kotlinx.coroutines-cn)
