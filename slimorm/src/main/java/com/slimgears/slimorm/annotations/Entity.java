// Copyright 2015 Denis Itskovich
// Refer to LICENSE.txt for license details
package com.slimgears.slimorm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Denis on 02-Apr-15
 * <File Description>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Entity {
}
