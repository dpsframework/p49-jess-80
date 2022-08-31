package jess.jsr94;

import javax.rules.*;
import javax.rules.admin.RuleAdministrator;

/**
 * This class is the main entry point for Jess's JSR94 driver. See the
 * Jess manual for usage information.
 * <p>
 * (C) 2013 Sandia Corporation<br>
 */
public class RuleServiceProviderImpl extends RuleServiceProvider {
    private RuleRuntimeImpl m_runtime;
    private RuleAdministratorImpl m_administrator;

    static {
        try {
            RuleServiceProviderManager.registerRuleServiceProvider("jess.jsr94", RuleServiceProviderImpl.class);
        } catch (ConfigurationException e) {
            throw new IllegalStateException("Can't register provider class: " + e.getMessage());
        }
    }

    public synchronized RuleRuntime getRuleRuntime() throws ConfigurationException {
        if (m_runtime == null)
            createMembers();
        return m_runtime;
    }

    private void createMembers() {
        m_administrator = new RuleAdministratorImpl();
        m_runtime = new RuleRuntimeImpl(m_administrator);

    }

    public synchronized RuleAdministrator getRuleAdministrator() throws ConfigurationException {
        if (m_administrator == null)
            createMembers();
        return m_administrator;
    }
}
