package org.opennms.netmgt.discovery.heartbeat;

import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;

import org.apache.camel.Component;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.seda.SedaComponent;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.camel.util.KeyValueHolder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.test.OpenNMSJUnit4ClassRunner;
import org.opennms.core.test.db.annotations.JUnitTemporaryDatabase;
import org.opennms.netmgt.dao.api.MinionDao;
import org.opennms.netmgt.dao.hibernate.MinionDaoHibernate;
import org.opennms.netmgt.model.minion.OnmsMinion;
import org.opennms.test.JUnitConfigurationEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@RunWith(OpenNMSJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/META-INF/opennms/applicationContext-soa.xml",
        "classpath:/META-INF/opennms/applicationContext-dao.xml",
        "classpath*:/META-INF/opennms/component-dao.xml",
        "classpath:/META-INF/opennms/applicationContext-commonConfigs.xml",
        "classpath:/META-INF/opennms/applicationContext-minimal-conf.xml" })
@JUnitConfigurationEnvironment
@JUnitTemporaryDatabase
public class HeartBeatBluePrintIT extends CamelBlueprintTestSupport {

    /**
     * Use Aries Blueprint synchronous mode to avoid a blueprint deadlock bug.
     * 
     * @see https://issues.apache.org/jira/browse/ARIES-1051
     * @see https://access.redhat.com/site/solutions/640943
     */
    @Autowired
    private MinionDao m_minionDao;

    @Override
    public void doPreSetup() throws Exception {
        System.setProperty("org.apache.aries.blueprint.synchronous",
                           Boolean.TRUE.toString());
        System.setProperty("de.kalpatec.pojosr.framework.events.sync",
                           Boolean.TRUE.toString());
    }

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @Override
    public boolean isUseDebugger() {
        // must enable debugger
        return true;
    }

    @Override
    public String isMockEndpoints() {
        return "*";
    }

    // The location of our Blueprint XML file to be used for testing
    @Override
    protected String getBlueprintDescriptor() {
        return "file:blueprint-heartbeat.xml";
    }

    /**
     * Register a mock OSGi {@link SchedulerService} so that we can make sure
     * that the scheduler whiteboard is working properly.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected void addServicesOnStartup(
            Map<String, KeyValueHolder<Object, Dictionary>> services) {

        services.put(MinionDao.class.getName(),
                     new KeyValueHolder(new MinionDaoHibernate(), null));

        Properties props = new Properties();
        props.setProperty("alias", "onms.broker");
        services.put(Component.class.getName(),
                     new KeyValueHolder<Object, Dictionary>(new SedaComponent(),
                                                            props));

    }

    @Test
    public void testHeartBeat() throws Exception {

        MockEndpoint heartBeatqueue = getMockEndpoint("mock:queuingservice:heartBeat",
                                                      false);
        heartBeatqueue.setExpectedMessageCount(1);

        TestMinion minion = new TestMinion("localhost", "minion");
        template.sendBody("queuingservice:heartBeat", minion);

        Thread.sleep(1000);
        assertMockEndpointsSatisfied();

        OnmsMinion result = m_minionDao.findById("minion");

        assertEquals("localhost", result.getLocation());

    }

}
