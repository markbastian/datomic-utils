(ns generic.utils.lambda
  (:require [cognitect.aws.client.api :as aws]
            [clojure.string :as cs]
            [cheshire.core :as ch]
            [clojure.java.io :as io]))

(defonce lambda (aws/client {:api :lambda}))

(defn function-names []
  (->> (:Functions (aws/invoke lambda {:op :ListFunctions}))
       (map :FunctionName)
       sort))

(defn invoke [fn-name & [args]]
  (let [request (cond-> {:FunctionName fn-name} args (assoc :Payload (ch/encode args)))]
    (with-open [r (io/reader (:Payload (aws/invoke lambda {:op :Invoke :request request})))]
      (ch/parse-stream r))))