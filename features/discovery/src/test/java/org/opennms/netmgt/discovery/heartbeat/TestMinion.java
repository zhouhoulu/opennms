package org.opennms.netmgt.discovery.heartbeat;

import java.io.Serializable;
import java.util.Objects;

import org.opennms.minion.core.api.*;

public class TestMinion implements Serializable, MinionIdentity {

	/**
	 * 
	 */
	private final String m_location;
    private final String m_id;

	public TestMinion(String location, String id) {
        m_location = Objects.requireNonNull(location);
        m_id = Objects.requireNonNull(id);
    }
	private static final long serialVersionUID = -3407238549310761325L;

	@Override
	public String getId() {
		return m_id;
	}

	@Override
	public String getLocation() {
		 return m_location;
	}

}
