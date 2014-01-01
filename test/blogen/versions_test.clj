(ns blogen.versions-test
  (:use clojure.test
        [clj-time.core :only [date-time]]
        blogen.versions))

(def test-msg'
"%HASH: 8f56bbb 
%DATE: Wed, 4 Dec 2013 20:16:00 +0200
%NOTE: Monoliittinen blog.org hajotettu tiedostoiksi. ")

(def test-msg-2
"%HASH: afe423
%DATE: Wed, 4 Dec 2013 20:16:00 +0000
%NOTE: [MAJOR] On new clojurian methods!")

(deftest revisions
  (testing "Reading a rev from git log"
    (is (= {:hash "afe423"
            :major? true
            :date (date-time 2013 12 4 20 16 00)
            :note "[MAJOR] On new clojurian methods!"}
           (read-revision test-msg-2)))
    (is (= {:hash "8f56bbb"
            :major? false
            :date (date-time 2013 12 4 18 16 00)
            :note "Monoliittinen blog.org hajotettu tiedostoiksi."}
           (read-revision test-msg')))))