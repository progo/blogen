(ns blogen.config)

;; org output
(def blog-dir "/home/progo/temp/blog/")

;; Where the original material lies
(def original-dir "/home/progo/dokumentit/blog/")

;; HTML/enlive templates
(def template-dir "/home/progo/dokumentit/blog/templates/")

;; Set of full path patterns that are to be excluded
(def excludes #{#"^.*/templates/.*html$"})

;; Relative location of the main CSS
(def main-css-location "/templates/main.css")

(def site-title "Foobarly adventures")