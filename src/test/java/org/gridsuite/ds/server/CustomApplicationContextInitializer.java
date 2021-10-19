/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server;

import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author Jacques Borsenberger <jacques.borsenberger at rte-france.com>
*/

public class CustomApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    /* setAllowRawInjectionDespiteWrapping(true) because test use spybean on self injecting bean (DynamicSimulationWorkerService)
    without lazy or allowRawInjectionDespiteWrapping springs fails with
    org.springframework.beans.factory.BeanCurrentlyInCreationException: Error creating bean with name
        'dynamicSimulationWorkerService': Bean with name 'dynamicSimulationWorkerService' has been injected into other
        beans [dynamicSimulationWorkerService] in its raw version as part of a circular reference, but has eventually
        been wrapped. This means that said other beans do not use the final version of the bean. This is often the
        result of over-eager type matching - consider using 'getBeanNamesForType'
        with the 'allowEagerInit' flag turned off, for example.

    allowRawInjectionDespiteWrapping is not a perfect solution because self is still vanilla  DynamicSimulationWorkerService, and not the mocked bean
        however it works for us now because we only mock function not called on self
    if this causes problems in the future we will need to find a better fix */

    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
        var factory = configurableApplicationContext.getBeanFactory();
        if (factory instanceof AbstractAutowireCapableBeanFactory) {
            ((AbstractAutowireCapableBeanFactory) factory).setAllowRawInjectionDespiteWrapping(true);
        }
    }
}
