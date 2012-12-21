/*
 * Copyright 2011 JBoss, by Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.codegen.meta.impl.java;

import java.lang.reflect.Constructor;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;

import org.jboss.errai.codegen.meta.MetaClassFactory;
import org.jboss.errai.codegen.meta.MetaGenericDeclaration;
import org.jboss.errai.codegen.meta.MetaType;
import org.jboss.errai.codegen.meta.MetaTypeVariable;

/**
 * Java Reflection based implementation of {@link MetaTypeVariable}.
 *
 * @author Mike Brock <cbrock@redhat.com>
 * @author Jonathan Fuerth <jfuerth@redhat.com>
 */
public class JavaReflectionTypeVariable implements MetaTypeVariable {
  private final TypeVariable<?> variable;
  private final MetaGenericDeclaration declaration;

  public JavaReflectionTypeVariable(final TypeVariable<?> variable) {
    this.variable = variable;

    // TODO return the MetaClass, MetaConstructor, or MetaMethod where this variable was declared
    GenericDeclaration genericDeclaration = variable.getGenericDeclaration();
    if (genericDeclaration instanceof Class) {
      this.declaration = MetaClassFactory.get((Class<?>) genericDeclaration);
    }
    else if (genericDeclaration instanceof Method) {
      this.declaration = MetaClassFactory.get((Method) genericDeclaration);
    }
    else if (genericDeclaration instanceof Method) {
      this.declaration = new JavaReflectionConstructor((Constructor<?>) genericDeclaration);
    }
    else {
      throw new RuntimeException("Generic declaration was of an unexpected type: " + genericDeclaration.getClass());
    }
  }

  @Override
  public MetaType[] getBounds() {
    return JavaReflectionUtil.fromTypeArray(variable.getBounds());
  }

  @Override
  public String getName() {
    return variable.getName();
  }

  @Override
  public MetaGenericDeclaration getGenericDeclaration() {
    return declaration;
  }
}
