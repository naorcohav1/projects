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

(provide (all-defined-out))
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


;;; Q2.1
(check-equal? (take (as-real 4) 8) '(4 4 4 4 4 4 4 4) "incorrect as-real 1")
(check-equal? (take (as-real 3) 2) '(3 3) "incorrect as-real 2")
(check-equal? (take (++ (integers-from 0) (integers-from 10)) 5) '(10 12 14 16 18) "incorrect ++ 1")
(letrec ((r3 (cons-lzl 3 (lambda () r3))) (r5 (cons-lzl 5 (lambda () r5))))
  (check-equal? (take (++ r3 r5) 4) '(8 8 8 8) "incorrect ++ 2")
)
(check-equal? (take (-- (integers-from 10) (integers-from 2)) 5) '(8 8 8 8 8) "incorrect -- 1")
(letrec ((r10 (cons-lzl 10 (lambda () r10))) (r2 (cons-lzl 2 (lambda () r2))))
  (check-equal? (take (-- r10 r2) 4) '(8 8 8 8) "incorrect -- 2")
)
(check-equal? (take (** (integers-from 10) (integers-from 2)) 4) '(20 33 48 65) "incorrect ** 1")
(letrec ((r4 (cons-lzl 4 (lambda () r4))))
  (check-equal? (take (** r4 (integers-from 0)) 6) '(0 4 8 12 16 20) "incorrect ** 2")
)
(letrec ((r3 (cons-lzl 3 (lambda () r3))) (r2 (cons-lzl 2 (lambda () r2))))
  (check-equal? (take (** r3 r2) 8) '(6 6 6 6 6 6 6 6) "incorrect ** 3")
)
(check-equal? (take (// (integers-from 10) (integers-from 1)) 5) '(10/1 11/2 12/3 13/4 14/5) "incorrect // 1")
(letrec ((r10 (cons-lzl 10 (lambda () r10))) (r2 (cons-lzl 2 (lambda () r2))))
  (check-equal? (take (// r10 r2) 4) '(5 5 5 5) "incorrect // 2")
)
(letrec ((r1 (cons-lzl 1 (lambda () r1))))
  (check-equal? (take (// (integers-from 0) r1) 8) (take (integers-from 0) 8) "incorrect // 3")
)