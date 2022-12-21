package lib.annotation;

import lib.annotation.enums.CascadeType;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static lib.annotation.FetchType.LAZY;

@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface OneToMany {

    Class targetEntity() default void.class;

    CascadeType[] cascade() default {};


    FetchType fetch() default LAZY;


    String mappedBy() default "";


    boolean orphanRemoval() default false;
}
