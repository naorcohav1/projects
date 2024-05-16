#lang racket
(require rackunit)

(define make-tree list)
(define add-subtree cons)
(define make-leaf (lambda (x) x))
(define empty-tree? empty?)
(define first-subtree car)
(define rest-subtree cdr)
(define leaf-data (lambda (x) x))
(define composite-tree? list?)
(define leaf? (lambda (x) (not (list? x))))

(provide (all-defined-out))

(define integers-from
  (lambda (n)
    (cons-lzl n (lambda () (integers-from (+ n 1))))))

(define cons-lzl cons)
(define empty-lzl? empty?)
(define empty-lzl '())
(define head car)
(define tail
  (lambda (lzl)
    ((cdr lzl))))



;; Signature: map-lzl(f, lz)
;; Type: [[T1 -> T2] * Lzl(T1) -> Lzl(T2)]
(define map-lzl
  (lambda (f lzl)
    (if (empty-lzl? lzl)
        lzl
        (cons-lzl (f (head lzl))
                  (lambda () (map-lzl f (tail lzl)))))))

;; Signature: take(lz-lst,n)
;; Type: [LzL*Number -> List]
;; If n > length(lz-lst) then the result is lz-lst as a List
(define take
  (lambda (lz-lst n)
    (if (or (= n 0) (empty-lzl? lz-lst))
      empty-lzl
      (cons (head lz-lst)
            (take (tail lz-lst) (- n 1))))))

; Signature: nth(lz-lst,n)
;; Type: [LzL*Number -> T]
;; Pre-condition: n < length(lz-lst)
(define nth
  (lambda (lz-lst n)
    (if (= n 0)
        (head lz-lst)
        (nth (tail lz-lst) (- n 1)))))


;;; Q1.1
; Signature: append$(lst1, lst2, cont) 
; Type: [List * List * [List -> T]] -> T
; Purpose: Returns the concatination of the given two lists, with cont pre-processing
(define append$
  (lambda (lst1 lst2 cont)
    (if (empty? lst1)
        (cont lst2)
        (append$ (cdr lst1) lst2
                 (lambda (result)
                   (cont (cons (car lst1) result)))))
  )
)
;;; Q1.2
; Signature: equal-trees$(tree1, tree2, succ, fail) 
; Type: [Tree * Tree * [Tree ->T1] * [Pair->T2] -> T1 U T2
; Purpose: Determines the structure identity of a given two lists, with post-processing succ/fail
(define (equal-trees$ tree1 tree2 succ fail)
  (cond
    ;; If both trees are empty, return an empty tree
    ((and (empty-tree? tree1) (empty-tree? tree2))
     (succ '()))

    ;; If both trees are compound trees, recursively check their subtrees
    ((and (composite-tree? tree1) (composite-tree? tree2))
     (define (first-subtree1) (car tree1))
     (define (first-subtree2) (car tree2))
  (equal-trees$ (first-subtree1) (first-subtree2) (lambda (first)
      (equal-trees$ (rest-subtree tree1) (rest-subtree tree2) (lambda (second) (succ (cons first second))) fail)) fail))
    ;; If both trees are atomic leaves, create a pair of their values
    ((and (leaf? tree1) (leaf? tree2))
     (succ (cons (leaf-data tree1) (leaf-data tree2))))
    ((or (leaf? tree1) (leaf? tree2)) (fail (cons (leaf-data tree1) (leaf-data tree2))))))
;;; Q2.1

;; Signature: as-real(x)
;; Type: [ Number -> Lzl(Number) ]
;; Purpose: Convert a rational number to its form as a
;; constant real number
(define as-real
  (lambda (x)
    (define fun (lambda () (as-real x)))
    (cons-lzl x fun)
  )
)

;; Signature: ++(x, y)
;; Type: [ Lzl(Number) * Lzl(Number) -> Lzl(Number) ]
;; Purpose: Addition of real numbers
(define ++
  (lambda (x y)
    (define addFun (lambda () (++ (tail x) (tail y)))) 
    (cons-lzl (+ (car x) (car y)) addFun))
  )

;; Signature: --(x, y)
;; Type: [ Lzl(Number) * Lzl(Number) -> Lzl(Number) ]
;; Purpose: Subtraction of real numbers
(define --
  (lambda (x y)
    (define minFun (lambda () (-- (tail x) (tail y)))) 
    (cons-lzl (- (car x) (car y)) minFun))
  )

;; Signature: **(x, y)
;; Type: [ Lzl(Number) * Lzl(Number) -> Lzl(Number) ]
;; Purpose: Multiplication of real numbers
(define **
  (lambda (x y)
    (define mulFun (lambda () (** (tail x) (tail y)))) 
    (cons-lzl (* (car x) (car y)) mulFun))
  )
;; Signature: //(x, y)
;; Type: [ Lzl(Number) * Lzl(Number) -> Lzl(Number) ]
;; Purpose: Division of real numbers
(define //
  (lambda (x y)
    (define divFun (lambda () (// (tail x) (tail y)))) 
    (cons-lzl (/ (car x) (car y)) divFun))
  )


;;; Q2.2.a
;; Signature: sqrt-with(x y)
;; Type: [ Lzl(Number) * Lzl(Number) -> Lzl(Lzl(Number)) ]
;; Purpose: Using an initial approximation `y`, return a 
;; sequence of real numbers which converges into the 
;; square root of `x`
(define sqrt-with
  (lambda (x y)
    (define sqrtFun (lambda () (sqrt-with x (// (++ (** y y) x) (** (as-real 2) y)))))
    (cons-lzl y sqrtFun)
  )
)
;;; Q2.2.b
;; Signature: diag(lzl)
;; Type: [ Lzl(Lzl(T)) -> Lzl(T) ]
;; Purpose: Diagonalize an infinite lazy list
(define diag
  (lambda (lzl)
   (define diagFun (lambda() (diag (map-lzl (lambda (lzl1) (tail lzl1)) (tail lzl)))))
    (cons-lzl (head (head lzl))diagFun) 
  )
)

;;; Q2.2.c
;; Signature: rsqrt(x)
;; Type: [ Lzl(Number) -> Lzl(Number) ]
;; Purpose: Take a real number and return its square root
;; Example: (take (rsqrt (as-real 4.0)) 6) => '(4.0 2.5 2.05 2.0006097560975613 2.0000000929222947 2.000000000000002)
(define rsqrt
  (lambda (x)
    (diag (sqrt-with x x))
  )
)

;;; Q2.2.c
(letrec ((r4 (cons-lzl 4.0 (lambda () r4))))
  (check-equal? (< (abs (- (nth (rsqrt r4) 6) 2.0)) 0.001) #t "incorrect rsqrt 1")
)
(letrec ((decay (cons-lzl 1 (lambda () (map-lzl (lambda (x) (/ x 10.0)) decay)))))
  (check-equal? (< (nth (rsqrt decay) 6) 0.1) #t "incorrect rsqrt 2")
)
