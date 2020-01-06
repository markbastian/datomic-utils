# Useful tools-deps commands for Datomic
Datomic is super awesome, but the tooling around it could be improved. This project adds some useful commands.

## About
These utilities make use of David Chelimsky's awesome [Cognitect AWS API](https://github.com/cognitect-labs/aws-api) so that you do not need to have the aws cli tools installed. It's all Clojure!

A good place to find the latest releases of all APIs can be found [here](https://github.com/cognitect-labs/aws-api/blob/master/latest-releases.edn).

## Commands
 * `clj -m list-instances`: List runing Datomic systems.
 * `clj -m shutdown-system --system $YOUR_SYSTEM_NAME_HERE`: Set all autoscaling values (min, max, desired) to 0 for a named system.