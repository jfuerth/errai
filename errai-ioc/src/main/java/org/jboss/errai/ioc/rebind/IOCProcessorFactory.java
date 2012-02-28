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

package org.jboss.errai.ioc.rebind;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.jboss.errai.codegen.framework.meta.MetaClass;
import org.jboss.errai.codegen.framework.meta.MetaClassFactory;
import org.jboss.errai.codegen.framework.meta.MetaField;
import org.jboss.errai.codegen.framework.meta.MetaMethod;
import org.jboss.errai.common.client.graph.Digraph;
import org.jboss.errai.common.client.graph.TopologicalSort;
import org.jboss.errai.common.metadata.MetaDataScanner;
import org.jboss.errai.common.rebind.EnvironmentUtil;
import org.jboss.errai.ioc.client.api.TestOnly;
import org.jboss.errai.ioc.rebind.ioc.InjectableInstance;
import org.jboss.errai.ioc.rebind.ioc.InjectionFailure;
import org.jboss.errai.ioc.rebind.ioc.Injector;
import org.jboss.errai.ioc.rebind.ioc.InjectorFactory;
import org.jboss.errai.ioc.rebind.ioc.util.WiringUtil;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.jboss.errai.ioc.rebind.ioc.InjectableInstance.getMethodInjectedInstance;
import static org.jboss.errai.ioc.rebind.ioc.InjectableInstance.getTypeInjectedInstance;

public class IOCProcessorFactory {
  private SortedSet<ProcessingEntry> processingEntries = new TreeSet<ProcessingEntry>();
  private Map<SortUnit, SortUnit> delegates = new LinkedHashMap<SortUnit, SortUnit>();
  private Multimap<SortUnit, SortUnit> reverseDependenciesMap = HashMultimap.create();

  private InjectorFactory injectorFactory;

  public IOCProcessorFactory(InjectorFactory factory) {
    this.injectorFactory = factory;
  }

  public void registerHandler(Class<? extends Annotation> annotation, AnnotationHandler handler) {
    processingEntries.add(new ProcessingEntry(annotation, handler));
  }

  public void registerHandler(Class<? extends Annotation> annotation, AnnotationHandler handler, List<RuleDef> rules) {
    processingEntries.add(new ProcessingEntry(annotation, handler, rules));
  }

  private void addToDelegates(SortUnit unit) {
    if (delegates.containsKey(unit)) {
      SortUnit existing = delegates.get(unit);
      for (Object o : unit.getItems()) {
        existing.addItem(o);
      }
    }
    else {
      delegates.put(unit, unit);
    }
  }

  @SuppressWarnings({"unchecked"})
  public void process(final MetaDataScanner scanner, final IOCProcessingContext context) {
    /**
     * Let's accumulate all the processing tasks.
     */
    for (final ProcessingEntry entry : processingEntries) {
      Class<? extends Annotation> aClass = entry.annotationClass;
      Target target = aClass.getAnnotation(Target.class);

      if (target == null) {
        target = new Target() {
          @Override
          public ElementType[] value() {
            return new ElementType[]
                    {ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.FIELD,
                            ElementType.METHOD, ElementType.FIELD};
          }

          @Override
          public Class<? extends Annotation> annotationType() {
            return Target.class;
          }
        };
      }

      class DependencyControlImpl implements DependencyControl {
        private MetaClass masqueradeClass;

        @Override
        public void masqueradeAs(MetaClass clazz) {
          masqueradeClass = clazz;
        }
      }

      for (ElementType elementType : target.value()) {
        final DependencyControlImpl dependencyControl = new DependencyControlImpl();

        switch (elementType) {
          case TYPE: {
            Set<Class<?>> classes = scanner.getTypesAnnotatedWith(aClass, context.getPackages());
            for (final Class<?> clazz : classes) {
              final Annotation anno = clazz.getAnnotation(aClass);

              final MetaClass type = MetaClassFactory.get(clazz);
              dependencyControl.masqueradeAs(type);

              if (type.isAnnotationPresent(TestOnly.class) && !EnvironmentUtil.isGWTJUnitTest()) {
                continue;
              }


              ProcessingDelegate<MetaClass> del = new ProcessingDelegate<MetaClass>() {
                @Override
                public Set<SortUnit> getRequiredDependencies() {
                  final InjectableInstance injectableInstance
                          = getTypeInjectedInstance(anno, type, null, injectorFactory.getInjectionContext());

                  return entry.handler.checkDependencies(dependencyControl, injectableInstance, anno, context);
                }

                @Override
                public boolean process() {
                  injectorFactory.addType(type);

                  Injector injector = injectorFactory.getInjectionContext().getInjector(type);
                  final InjectableInstance injectableInstance
                          = getTypeInjectedInstance(anno, type, injector, injectorFactory.getInjectionContext());

                  return entry.handler.handle(injectableInstance, anno, context);
                }

                public MetaClass getType() {
                  return type;
                }

                public boolean equals(Object o) {
                  return o != null && toString().equals(o.toString());
                }


                public String toString() {
                  return clazz.getName();
                }
              };

              entry.addProcessingDelegate(del);
              Set<SortUnit> requiredDependencies = del.getRequiredDependencies();
              addToDelegates(new SortUnit(dependencyControl.masqueradeClass, del, requiredDependencies));
            }
          }
          break;

          case METHOD: {
            Set<Method> methods = scanner.getMethodsAnnotatedWith(aClass, context.getPackages());

            for (Method method : methods) {
              final Annotation anno = method.getAnnotation(aClass);
              final MetaClass type = MetaClassFactory.get(method.getDeclaringClass());
              final MetaMethod metaMethod = MetaClassFactory.get(method);

              dependencyControl.masqueradeAs(type);


              ProcessingDelegate<MetaField> del = new ProcessingDelegate<MetaField>() {
                @Override
                public Set<SortUnit> getRequiredDependencies() {
                  final InjectableInstance injectableInstance
                          = getMethodInjectedInstance(anno, metaMethod, null,
                          injectorFactory.getInjectionContext());

                  return entry.handler.checkDependencies(dependencyControl, injectableInstance, anno, context);
                }

                @Override
                public boolean process() {
                  injectorFactory.addType(type);

                  Injector injector = injectorFactory.getInjectionContext().getInjector(type);
                  final InjectableInstance injectableInstance
                          = getMethodInjectedInstance(anno, metaMethod, injector,
                          injectorFactory.getInjectionContext());


                  return entry.handler.handle(injectableInstance, anno, context);
                }

                public MetaClass getType() {
                  return type;
                }

                public boolean equals(Object o) {
                  return o != null && toString().equals(o.toString());
                }


                public String toString() {
                  return type.getFullyQualifiedName();
                }

              };

              entry.addProcessingDelegate(del);
              Set<SortUnit> requiredDependencies = del.getRequiredDependencies();
              addToDelegates(new SortUnit(dependencyControl.masqueradeClass, del, requiredDependencies));
            }
          }
          break;

          case FIELD: {
            Set<Field> fields = scanner.getFieldsAnnotatedWith(aClass, context.getPackages());

            for (Field method : fields) {
              final Annotation anno = method.getAnnotation(aClass);

              final MetaClass type = MetaClassFactory.get(method.getDeclaringClass());
              final MetaField metaField = MetaClassFactory.get(method);
              dependencyControl.masqueradeAs(type);


              ProcessingDelegate<MetaField> del = new ProcessingDelegate<MetaField>() {
                @Override
                public Set<SortUnit> getRequiredDependencies() {
                  final InjectableInstance injectableInstance
                          = InjectableInstance.getFieldInjectedInstance(anno, metaField, null,
                          injectorFactory.getInjectionContext());

                  return entry.handler.checkDependencies(dependencyControl, injectableInstance, anno, context);
                }

                @Override
                public boolean process() {
                  injectorFactory.addType(type);

                  Injector injector = injectorFactory.getInjectionContext().getInjector(type);
                  final InjectableInstance injectableInstance
                          = InjectableInstance.getFieldInjectedInstance(anno, metaField, injector,
                          injectorFactory.getInjectionContext());

                  return entry.handler.handle(injectableInstance, anno, context);
                }

                public MetaClass getType() {
                  return type;
                }

                public boolean equals(Object o) {
                  return o != null && toString().equals(o.toString());
                }


                public String toString() {
                  return type.getFullyQualifiedName();
                }

              };

              entry.addProcessingDelegate(del);
              Set<SortUnit> requiredDependencies = del.getRequiredDependencies();
              addToDelegates(new SortUnit(dependencyControl.masqueradeClass, del, requiredDependencies));
            }
          }
        }
      }
    }

    for (SortUnit del : delegates.keySet()) {
      for (SortUnit requiredDependency : del.getDependencies()) {
        reverseDependenciesMap.put(requiredDependency, del);
      }
    }

    Digraph<SortUnit> processingDelegateDigraph = new Digraph<SortUnit>() {
      @Override
      public Set<SortUnit> getNodes() {
        return delegates.keySet();
      }

      @Override
      public Set<SortUnit> nodesReferencedFrom(SortUnit fromNode) {
        return fromNode.getDependencies();
      }

      @Override
      public Set<SortUnit> nodesReferencing(SortUnit toNode) {
        return new HashSet<SortUnit>(reverseDependenciesMap.get(toNode));
      }
    };


//    List<SortUnit> list = TopologicalSort.topologicalSort(processingDelegateDigraph);
//    Collections.reverse(list);

    List<SortUnit> list = WiringUtil.worstSortAlgorithmEver(delegates.keySet());

    for (SortUnit unit : list) {
      for (Object item : unit.getItems()) {
        if (item instanceof ProcessingDelegate) {
          ((ProcessingDelegate) item).process();
        }
      }
    }
  }


  private class ProcessingEntry<T> implements Comparable<ProcessingEntry> {
    private Class<? extends Annotation> annotationClass;
    private AnnotationHandler handler;
    private Set<RuleDef> rules;
    private List<ProcessingDelegate<T>> targets = new ArrayList<ProcessingDelegate<T>>();
    private Set<InjectionFailure> errors = new LinkedHashSet<InjectionFailure>();

    private ProcessingEntry(Class<? extends Annotation> annotationClass, AnnotationHandler handler) {
      this.annotationClass = annotationClass;
      this.handler = handler;
    }

    private ProcessingEntry(Class<? extends Annotation> annotationClass, AnnotationHandler handler,
                            List<RuleDef> rule) {
      this.annotationClass = annotationClass;
      this.handler = handler;
      this.rules = new HashSet<RuleDef>(rule);
    }

    public boolean processAllDelegates() {
      int start;

      errors.clear();

      do {
        start = targets.size();

        Iterator<ProcessingDelegate<T>> iterator = targets.iterator();

        ProcessingDelegate<T> delegate;

        while (iterator.hasNext()) {
          try {
            delegate = iterator.next();

            if (delegate.process()) {
              iterator.remove();
            }
          }
          catch (InjectionFailure f) {
            errors.add(f);
            // Ignored, see processAll()
          }
        }

      }
      while (!targets.isEmpty() && targets.size() < start);

      return targets.isEmpty();
    }

    public void addProcessingDelegate(ProcessingDelegate<T> delegate) {
      targets.add(delegate);
    }

    public Collection<InjectionFailure> getErrorList() {
      return Collections.unmodifiableCollection(errors);
    }

    @Override
    public int compareTo(ProcessingEntry processingEntry) {
      if (rules != null) {
        for (RuleDef def : rules) {
          if (!def.relAnnotation.equals(annotationClass)) {
            continue;
          }

          switch (def.order) {
            case After:
              return 1;
            case Before:
              return -1;
          }
        }
      }
      else if (processingEntry.rules != null) {
        for (RuleDef def : (Set<RuleDef>) processingEntry.rules) {
          if (!def.relAnnotation.equals(annotationClass)) {
            continue;
          }

          switch (def.order) {
            case After:
              return -1;
            case Before:
              return 1;
          }
        }
      }

      return -1;
    }

    public List<ProcessingDelegate<T>> getTargets() {
      return targets;
    }

    public String toString() {
      return "Scope:" + annotationClass.getName() + "(" + targets.toString() + "):\n" + targets;
    }
  }

  static class RuleDef {
    private Class<? extends Annotation> relAnnotation;
    private RelativeOrder order;

    RuleDef(Class<? extends Annotation> relAnnotation, RelativeOrder order) {
      this.relAnnotation = relAnnotation;
      this.order = order;
    }
  }

  private static interface ProcessingDelegate<T> {
    public boolean process();

    public Set<SortUnit> getRequiredDependencies();
  }
}
