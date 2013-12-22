(ns blogen.versions-test
  (:use clojure.test
        [clj-time.core :only [date-time]]
        blogen.versions))

(def test-msg'
"%HASH: 8f56bbb 
%DATE: Wed, 4 Dec 2013 20:16:00 +0200
%NOTE: Monoliittinen blog.org hajotettu tiedostoiksi. ")

(deftest revisions
  (testing "Reading a rev from git log"
    (is (= {:hash "8f56bbb"
            :date (date-time 2013 12 4 18 16 00)
            :note "Monoliittinen blog.org hajotettu tiedostoiksi."}
           (read-revision test-msg')))))