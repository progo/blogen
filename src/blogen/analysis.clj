(ns blogen.analysis
  "Provide functionality for the second-phase analysis of post
  collection. Everything culminates in public function
  `analyze-posts'"
  (:use
   [blogen.config])
  (:require
   [clojure.set :as set]
   [blogen.sort :as sort]
   [blogen.post :as post]
   [blogen.versions :as versions]))

(defn neighboring-posts
  "Check and 'link' each post with their predecessors and successors."
  [posts]
  (let [creation-order (vec (sort sort/by-creation-date posts))]
    (for [post posts]
      (let [post-index (.indexOf creation-order post)]
        (-> post
            (assoc-in [:order-created :next]
                      (-> (get creation-order (dec post-index)) :path))
            (assoc-in [:order-created :prev]
                      (-> (get creation-order (inc post-index)) :path)))))))

(defn tag-usages
  "Chew tags of all posts into a map of tag-count."
  [posts]
  (let [all-tags (->> (mapcat :tags posts)
                     sort
                     (partition-by identity)
                     (map (juxt first count))
                     (into {}))]
    (map #(assoc % :all-tags all-tags) posts)))

;;; Related posts

(defn taglike
  "Form a set of various tag-like entities a post has."
  [post]
  (let [title-exploded
        (-> post
            :title
            .toLowerCase
            (.replaceAll "[^a-z0-9 ]" "")
            (.split " +")
            seq)]
    (set/union
     (set (:tags post))
     (set title-exploded))))

;; Stuff that shouldn't affect the relatedness.
(def ^:private stopwords
  #{"a" "the"})

(defn relatedness
  "Eval roughly an integer estimate of two posts' relatedness. Larger
  number: more related."
  [a b]
  (count
   (set/difference
    (set/intersection
     (taglike a)
     (taglike b))
    stopwords)))

(defn related-posts
  "Find most related posts for given post."
  [post posts]
  (->> (sort-by first
                (for [p posts]
                  [(relatedness post p) p]))
       (take-last 6)
       butlast
       (map second)
       reverse))

(defn find-related-posts
  "Find related posts for all posts."
  [posts]
  (for [post posts]
    (assoc post :related-posts (related-posts post posts))))

(defn analyze-posts
  "Given input seq of posts, collect and generalize data within the
  context of all posts."
  [posts]
  (-> posts
      neighboring-posts
      find-related-posts
      tag-usages))
