(ns generic.utils.ec2
  (:require [cognitect.aws.client.api :as aws]))

(defonce ec2 (aws/client {:api :ec2}))

(defn keypairs [] (:KeyPairs (aws/invoke ec2 {:op :DescribeKeyPairs})))

(comment
  (keypairs))