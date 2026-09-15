## DriverFactory.java

- ThreadLocal<WebDriver> DRIVER — one browser per thread, no sharing.
- getDriver() — lazily creates a ChromeDriver on first call per thread, returns the same instance on subsequent calls within that thread.
- quitDriver() — quits the session and calls DRIVER.remove(). The remove matters: TestNG pools and reuses worker threads, so a stale ThreadLocal entry would hand a    
  later test a dead (already-quit) driver.
- WebDriverManager.chromedriver().setup() resolves and caches the matching chromedriver binary — no manual webdriver.chrome.driver path.
- Private constructor: it's a static utility, never instantiated.

  ---

Why a plain static WebDriver driver breaks under parallel="methods"

With parallel="methods", TestNG runs each @Test method on its own thread from a shared pool (size = thread-count). Say 3 tests run concurrently: threads T1, T2, T3.

One field, one slot. A static WebDriver driver is a single JVM-wide variable. All three threads read and write that same slot. There is no "T1's driver" and "T2's     
driver" — there is just driver, whatever value was written last.

Concrete failure sequence:

1. T1 enters its setup, does driver = new ChromeDriver() → Chrome window A. driver points to A.
2. T2 enters its setup, does driver = new ChromeDriver() → Chrome window B. driver now points to B. Window A's reference is gone, but the process is still alive —     
   orphaned.
3. T1 resumes its test body, calls driver.get(url). It's now driving window B, which T2 is also driving. Two threads issue commands down the same session: T1 navigates
   to the login page while T2 clicks a button that no longer exists → NoSuchElementException, StaleElementReferenceException, or wrong-page assertion failures. Flaky,
   and the flakiness moves around depending on thread timing.
4. T3 starts, overwrites driver again → window C.
5. Teardown: each thread calls driver.quit(). First quit() kills window C. The other two teardowns call quit() on an already-dead session → NoSuchSessionException.    
   Windows A and B are never closed — leaked browser processes that pile up every run.

Other broken variants fail too:
- Non-static instance field: TestNG creates one instance of the test class and reuses it across parallel method invocations, so an instance field is effectively shared
  the same way. Still one slot.
- synchronized around driver access: "fixes" the race by serializing everything — you've just thrown away the parallelism you configured.

Why ThreadLocal is the right tool: it gives each thread its own independent value for the same variable name. DRIVER.get() on T1 returns window A; DRIVER.get() on T2  
returns window B — same code, same static field, different backing storage keyed by Thread.currentThread(). The threads never see each other's browser, so there's no  
race to synchronize and no cross-talk. The one discipline it demands is remove() on teardown, because the pool outlives the test and would otherwise carry a dead      
session into the next method scheduled on that thread.

## Run comparison between Static WebDriver vs ThreadLocal
Both phases ran. Raw output, side by side:

================ PHASE 1: NAIVE static WebDriver field ================
pool-1-thread-1 -> asked for https://the-internet.herokuapp.com        | instance 301363089  | title=Example Domain |

pool-1-thread-2 -> asked for https://example.com                       | instance 301363089  | title=Example Domain |

pool-1-thread-3 -> asked for https://the-internet.herokuapp.com/login  | instance 1750372742 | title=The Internet   |

[main] naive field now points at instance 1750372742

[main] naive getter created 3 ChromeDriver instance(s) for 3 tasks

================ PHASE 2: ThreadLocal via DriverFactory ===============
pool-2-thread-1 -> asked for https://the-internet.herokuapp.com        | instance 1533046966 | title=The Internet   |

pool-2-thread-2 -> asked for https://example.com                       | instance 1094744044 | title=Example Domain |

pool-2-thread-3 -> asked for https://the-internet.herokuapp.com/login  | instance 1252760953 | title=The Internet   |

# What Phase 1 shows

┌────────────────────────┬─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐   
│        Symptom         │                                                           Evidence in output                                                            │   
├────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤   
│ Cross-talk / lost      │ thread-1 asked for the-internet.herokuapp.com but reports title=Example Domain. It shares instance 301363089 with thread-2, which       │   
│ navigation             │ navigated that same session to example.com. thread-1's getDriver() handed back the shared field, so its getTitle() sees thread-2's      │   
│                        │ page.                                                                                                                                   │   
├────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤   
│ Instance leak          │ 3 ChromeDrivers created for 3 tasks, but the field holds exactly one reference at a time. 2 browsers are orphaned — never quit by the   │   
│                        │ framework, process stays alive.                                                                                                         │   
├────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤   
│ Non-deterministic      │ The field ends on instance 1750372742 purely because that thread's assignment landed last. Rerun it and the numbers shuffle, but the    │   
│ final state            │ failure class is the same.                                                                                                              │   
└────────────────────────┴─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘

The if (naiveDriver == null) check-then-act is the unsynchronised race: every thread that evaluates the condition before some other thread's assignment becomes visible
will create its own browser; whichever threads evaluate it after an assignment silently share that browser.

# What Phase 2 shows

Every task's title matches the URL it asked for. Three distinct instances (1533046966, 1094744044, 1252760953), one per pool thread, each confined by the ThreadLocal  
and each quit() on its own thread via DriverFactory.quitDriver(). No sharing, no leak, nothing to synchronise.