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

import org.jboss.errai.codegen.framework.meta.MetaClass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Mike Brock
 */
public class SortUnit {
  private MetaClass type;
  private List<Object> items;
  private Set<SortUnit> dependencies;
  private boolean hard;

  public SortUnit(MetaClass type) {
    this.type = type;
    this.dependencies = Collections.emptySet();
    this.items = new ArrayList<Object>();
  }

  public SortUnit(MetaClass type, boolean hard) {
    this.type = type;
    this.dependencies = Collections.emptySet();
    this.items = new ArrayList<Object>();
    this.hard = true;
  }

  public SortUnit(MetaClass type, Object item) {
    this(type, item, Collections.<SortUnit>emptySet());
  }

  public SortUnit(MetaClass type, Object item, Set<SortUnit> dependencies) {
    this.type = type;
    this.items = new ArrayList<Object>();
    if (item != null) {
      this.items.add(item);
    }
    this.dependencies = dependencies;
  }

  public void addItem(Object item) {
    items.add(item);
  }

  public MetaClass getType() {
    return type;
  }

  public List<Object> getItems() {
    return items;
  }

  public Set<SortUnit> getDependencies() {
    return dependencies;
  }

  public boolean isHard() {
    return hard;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SortUnit)) return false;

    SortUnit sortUnit = (SortUnit) o;

    if (type != null ? !type.equals(sortUnit.type) : sortUnit.type != null) return false;

    return true;
  }


  @Override
  public int hashCode() {
    int result = type != null ? type.hashCode() : 0;
    return result;
  }

  @Override
  public String toString() {
    return type.toString() + " => " + dependencies;
  }
}
