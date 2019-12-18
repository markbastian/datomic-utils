# All about understanding tools deps
The [Deps and CLI Guide](https://clojure.org/guides/deps_and_cli) is actually pretty clear.

Basically, the formula is:
1. Create a top level ns (e.g. `foo`).
2. In that ns, create a standard main function. Note that it will only terminate if you explicitly call `(System/exit)` yourself.
3. Invoke the application as `clj -m name-of-ns`, e.g. `clj -m foo`.

To use these elsewhere:
1. Commit to git.
2. Add an alias to your deps.edn file. The alias can be whatever you want. Declare your dependency with the `:deps` key and specify the actual invocations with `:main-opts`.
3. Invoke the alias as `clj -A:alias`.

You can now invoke as:
 * `clj -m ls`
 * `clj -m list-instances`