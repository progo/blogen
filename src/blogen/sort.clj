(ns blogen.sort
  "Collect and implement various methods to sort the seq of posts."
  (:require
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

;; We didn't need to sort those revisions within one history again.
;; Git has us covered. (sort-by :date time/after? revs)
(defn- newest-revision-date
  [post]
  (-> (:history post) first :date))

(defn- newest-major-revision-date
  "Get the latest major rev date of a post. If there are no major
  revisions, take earliest revision."
  [post]
  (let [revs (:history post)
        major-revs (filter :major? revs)]
    (:date
     (if (seq major-revs)
       (first major-revs)
       (last revs)))))

(def by-latest-revision
  "Sort by most recent modifications. Order by desc."
  (make-post-comparator-func newest-revision-date time/after?))

(def by-latest-major-revision
  "Sort by most recent major modifications. Order by desc."
  (make-post-comparator-func newest-major-revision-date time/after?))

(def by-word-count
  "Sort by word count"
  (make-post-comparator-func :word-count >))