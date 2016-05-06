package org.opennms.netmgt.discovery.heartbeat;

import java.util.Date;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.opennms.minion.core.api.MinionIdentity;
import org.opennms.netmgt.dao.api.MinionDao;
import org.opennms.netmgt.model.minion.OnmsMinion;

public class UpdateHeartBeat implements Processor {

    private MinionDao minionDao;

    @Override
    public void process(Exchange exchange) throws Exception {

        final MinionIdentity minionHandle = exchange.getIn().getBody(MinionIdentity.class);
        String minionId = minionHandle.getId();
        String minionLocation = minionHandle.getLocation();
        OnmsMinion minion;
        if (minionDao != null) {
            minion = minionDao.findById(minionId);
            if (minion == null) {
                minion = new OnmsMinion();
                minion.setId(minionId);
                minion.setLocation(minionLocation);
                Date lastCheckedIn = new Date();
                minion.setLastCheckedIn(lastCheckedIn);
            }

            minionDao.saveOrUpdate(minion);
        }
    }

    public MinionDao getMinionDao() {
        return minionDao;
    }

    public void setMinionDao(MinionDao minionDao) {
        this.minionDao = minionDao;
    }

}
