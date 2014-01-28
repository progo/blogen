(ns blogen.sort
  "Collect and implement various methods to sort the seq of posts."
  (:require
   [blogen.utils :as utils]
   [clj-time.core :as time]))

(defn- make-post-comparator-func
  "Generalize over most of our sorting needs"
  [extractor-fn comparator-fn]
  (fn [A B]
    (let [d-A (extractor-fn A)
          d-B (extractor-fn B)]
      (comparator-fn d-A d-B))))

(def by-creation-date
  "Sort by the original creation date (not latest
  modification). Order is DESC."
  (make-post-comparator-func :created time/after?))

(def by-latest-revision
  "Sort by most recent modifications. Order by desc."
  (make-post-comparator-func
   utils/post-last-modified
   time/after?))

(def by-title
  "Sort by article title."
  (make-post-comparator-func
   (fn [p] (-> p
               :title
               .toLowerCase))
   compare))

(def by-latest-major-revision
  "Sort by most recent major modifications. Order by desc."
  (make-post-comparator-func
   utils/post-last-major-modified
   time/after?))

(def by-word-count
  "Sort by word count"
  (make-post-comparator-func :word-count >))