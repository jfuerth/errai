package org.jboss.errai.ui.nav.client.local;

import org.jboss.errai.enterprise.client.cdi.AbstractErraiCDITest;
import org.jboss.errai.ioc.client.container.IOC;
import org.jboss.errai.ioc.client.container.IOCBeanManager;
import org.jboss.errai.ui.nav.client.local.testpages.PageWithExtraState;
import org.jboss.errai.ui.nav.client.local.testpages.PageWithLifecycleMethods;

import com.google.common.collect.ImmutableMultimap;
import com.google.gwt.user.client.History;

public class PageLifecycleTest extends AbstractErraiCDITest {

  private IOCBeanManager beanManager = IOC.getBeanManager();
  private Navigation navigation;

  @Override
  public String getModuleName() {
    return "org.jboss.errai.ui.nav.NavigationTest";
  }

  @Override
  protected void gwtSetUp() throws Exception {
    disableBus = true;
    super.gwtSetUp();
    navigation = beanManager.lookupBean(Navigation.class).getInstance();
  }

  public void testPageShowingMethodCalled() throws Exception {
    PageWithLifecycleMethods page = beanManager.lookupBean(PageWithLifecycleMethods.class).getInstance();
    page.beforeShowCallCount = 0;

    navigation.goTo(PageWithLifecycleMethods.class, ImmutableMultimap.of("state", "foo"));

    assertEquals(1, page.beforeShowCallCount);
    assertEquals("foo", page.stateWhenBeforeShowWasCalled);
  }

  public void testPageHidingMethodCalled() throws Exception {
    PageWithLifecycleMethods page = beanManager.lookupBean(PageWithLifecycleMethods.class).getInstance();
    page.beforeHideCallCount = 0;

    navigation.goTo(PageWithLifecycleMethods.class, ImmutableMultimap.of("state", "foo"));
    assertEquals(0, page.beforeHideCallCount);

    navigation.goTo(PageWithExtraState.class, ImmutableMultimap.<String, String>of());
    assertEquals(1, page.beforeHideCallCount);

    // go back so we can check the snapshot
    History.back();
    assertTrue("History wasn't recorded properly. Was: " + History.getToken(),
            History.getToken().endsWith("#PageWithLifecycleMethods;state=lastMinuteChange"));
  }
}
