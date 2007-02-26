package org.opennms.dashboard.server;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.hibernate.criterion.Order;
import org.opennms.dashboard.client.Alarm;
import org.opennms.dashboard.client.SurveillanceData;
import org.opennms.dashboard.client.SurveillanceGroup;
import org.opennms.dashboard.client.SurveillanceService;
import org.opennms.dashboard.client.SurveillanceSet;
import org.opennms.netmgt.dao.AlarmDao;
import org.opennms.netmgt.dao.CategoryDao;
import org.opennms.netmgt.dao.GraphDao;
import org.opennms.netmgt.dao.NodeDao;
import org.opennms.netmgt.dao.OnmsDao;
import org.opennms.netmgt.dao.ResourceDao;
import org.opennms.netmgt.model.OnmsAlarm;
import org.opennms.netmgt.model.OnmsCategory;
import org.opennms.netmgt.model.OnmsCriteria;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.model.OnmsResource;
import org.opennms.netmgt.model.PrefabGraph;
import org.opennms.web.svclayer.ProgressMonitor;
import org.opennms.web.svclayer.SimpleWebTable;
import org.opennms.web.svclayer.SimpleWebTable.Cell;
import org.opennms.web.svclayer.dao.SurveillanceViewConfigDao;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Transactional(readOnly = true)
public class DefaultSurveillanceService implements SurveillanceService, InitializingBean {

    private NodeDao m_nodeDao;
    private ResourceDao m_resourceDao;
    private GraphDao m_graphDao;
    private org.opennms.web.svclayer.SurveillanceService m_webSurveillanceService;
    private SurveillanceViewConfigDao m_surveillanceViewConfigDao;
    private CategoryDao m_categoryDao;
    private AlarmDao m_alarmDao;

    private int m_count = 0;
    private Timer m_timer = new Timer();

    private Random m_random = new Random();
    
    private SurveillanceData m_data;

    
    public SurveillanceData getSurveillanceData() {
        /*
        System.err.println("Request made!");
        
        if (m_data == null) {
            System.err.println("Creating new data");
            final SurveillanceData data = new SurveillanceData();
            m_data = data;
            
            
            
        } else if (m_data.isComplete()) {
            SurveillanceData data = m_data;
            m_data = null;
            return data;
        }
        
        return m_data;
        */

        SurveillanceData data = new SurveillanceData();

        SimpleWebTable table = m_webSurveillanceService.createSurveillanceTable(null, new ProgressMonitor());
        
        List<SurveillanceGroup> columnGroups = new ArrayList<SurveillanceGroup>();
        for (Cell columnHeader : table.getColumnHeaders().subList(1, table.getColumnHeaders().size())) {
            SurveillanceGroup columnGroup = new SurveillanceGroup();
            columnGroup.setId(columnHeader.getContent().toString());
            columnGroup.setLabel(columnHeader.getContent().toString());
            columnGroup.setColumn(true);
            columnGroups.add(columnGroup);
        }
        data.setColumnGroups(columnGroups.toArray(new SurveillanceGroup[columnGroups.size()]));
        
        List<SurveillanceGroup> rowGroups = new ArrayList<SurveillanceGroup>();
        for (List<Cell> row : table.getRows()) {
            Cell rowHeader = row.get(0);
            
            SurveillanceGroup rowGroup = new SurveillanceGroup();
            rowGroup.setId(rowHeader.getContent().toString());
            rowGroup.setLabel(rowHeader.getContent().toString());
            rowGroups.add(rowGroup);
        }
        data.setRowGroups(rowGroups.toArray(new SurveillanceGroup[rowGroups.size()]));

        int rowIndex = 0;
        for (List<Cell> row : table.getRows()) {
            int columnIndex = 0;
            for (Cell cell : row.subList(1, row.size())) {
                data.setCell(rowIndex, columnIndex, cell.getContent().toString());
                
                columnIndex++;
            }
            rowIndex++;
        }
        
        data.setComplete(true);

        return data;
    }
    
    
    /*
    public SurveillanceData getSurveillanceData() {
        
        System.err.println("Request made!");
        
        if (m_data == null) {
            System.err.println("Creating new data");
            final SurveillanceData data = new SurveillanceData();
            m_data = data;
            
            SurveillanceGroup[] columnGroups = new SurveillanceGroup[] {
                    new SurveillanceGroup("prod", "Production"), 
                    new SurveillanceGroup("test", "Test"), 
                    new SurveillanceGroup("dev", "Developement")
            };
            
            SurveillanceGroup[] rowGroups = new SurveillanceGroup[] {
                    new SurveillanceGroup("ibm", "IBM"),
                    new SurveillanceGroup("hp", "HP"),
                    new SurveillanceGroup("duke", "Duke Hospital"),
                    new SurveillanceGroup("unc", "UNC Hospitals")
            };
            
            data.setColumnGroups(columnGroups);
            data.setRowGroups(rowGroups);
            
            m_timer.schedule(new TimerTask() {

                @Override
                public void run() {
                    
                    System.err.println("Updating data");
                    data.setCell(m_count / data.getColumnCount(), m_count % data.getColumnCount(), ""+m_count);
                    
                    m_count++;
                    
                    if (m_count < data.getColumnCount()*data.getRowCount()) {
                        data.setComplete(false);
                    } else {
                        this.cancel();
                        data.setComplete(true);
                        m_count = 0;
                    }

                }
                
            }, 3000, 2000);
        } else if (m_data.isComplete()) {
            SurveillanceData data = m_data;
            m_data = null;
            return data;
        }
        
        return m_data;

        
    }

*/

    public Alarm[] getAlarmsForSet(SurveillanceSet set) {
        OnmsCriteria criteria = new OnmsCriteria(OnmsAlarm.class, "alarm");
        OnmsCriteria nodeCriteria = criteria.createCriteria("node");
        addCriteriaForSurveillanceSet(nodeCriteria, set);
        
        List<OnmsAlarm> alarms = m_alarmDao.findMatching(criteria);

        Alarm[] alarmArray = new Alarm[alarms.size()];
        
        int index = 0;
        for (OnmsAlarm alarm : alarms) {
            alarmArray[index] = new Alarm(getSeverityString(alarm.getSeverity()), alarm.getNode().getLabel(), alarm.getDescription(), alarm.getCounter());
            index++;
        }
        
        return alarmArray;
    }
    
    /*
    public Alarm[] getAlarmsForSet(SurveillanceSet set) {
        try { Thread.sleep(2000); } catch (InterruptedException e) {}
        
        int alarmCount = m_random.nextInt(30);
        
        Alarm[] alarms = new Alarm[alarmCount];
        for(int i = 0; i < alarmCount; i++) {
            alarms[i] = newAlarm();
        }
        
        return alarms;
        
    }

    private Alarm newAlarm() {
        return new Alarm(getSeverity(m_random.nextInt(5)), "node"+m_random.nextInt(20), "An alarm", 2);
    }
    
    private String getSeverity(int count) {
        switch(count % 5) {
        case 0: return "Normal";
        case 1: return "Critical";
        case 2: return "Major";
        case 3: return "Minor";
        case 4: return "Resolved";
        default: return "Normal";
        }
    }
    */

    private String getSeverityString(Integer severity) {
        switch(severity) {
        case 1: return "Indeterminate";
        case 2: return "Cleared";
        case 3: return "Normal";
        case 4: return "Warning";
        case 5: return "Minor";
        case 6: return "Major";
        case 7: return "Critical";
        default: return "Unknown";
        }
    }


    public String[] getNodeNames(SurveillanceSet set) {

        List<OnmsNode> nodes = m_nodeDao.findAll();

        List<String> labels = new ArrayList<String>(nodes.size());
        for (OnmsNode node : nodes) {
            labels.add(node.getLabel());
        }

        return labels.toArray(new String[labels.size()]);

    }

    public String[][] getResources(SurveillanceSet set) {
        OnmsCriteria criteria = new OnmsCriteria(OnmsNode.class, "node");
        addCriteriaForSurveillanceSet(criteria, set);
        criteria.addOrder(Order.asc("node.label"));
        
        List<OnmsNode> nodes = m_nodeDao.findMatching(criteria);
        
        List<OnmsResource> resources = new ArrayList<OnmsResource>();
        for (OnmsNode node : nodes) {
            OnmsResource resource = m_resourceDao.getResourceForNode(node);
            if (resource != null) {
                resources.add(resource);
            }
        }
        
        List<String[]> labels = new ArrayList<String[]>(resources.size());
        for (OnmsResource resource : resources) {
            labels.add(new String[] { resource.getId(), resource.getResourceType().getLabel() + ": " + resource.getLabel() });
        }
        
        return labels.toArray(new String[labels.size()][]);
    }
    

    private void addCriteriaForSurveillanceSet(OnmsCriteria criteria, SurveillanceSet set) {
        CriteriaAddingVisitor visitor = new CriteriaAddingVisitor(criteria);
        visitor.setSurveillanceViewConfigDao(m_surveillanceViewConfigDao);
        visitor.setCategoryDao(m_categoryDao);

        set.visit(visitor);
    }


    public String[][] getChildResources(String id) {
        OnmsResource parentResource = m_resourceDao.getResourceById(id);
        if (parentResource == null) {
            return null;
        }
        
        List<OnmsResource> resources = parentResource.getChildResources();
        
        List<String[]> labels = new ArrayList<String[]>(resources.size());
        for (OnmsResource resource : resources) {
            labels.add(new String[] { resource.getId(), resource.getResourceType().getLabel() + ": " + resource.getLabel() });
        }
        
        return labels.toArray(new String[labels.size()][]);
    }

    public String[][] getPrefabGraphs(String id) {
        OnmsResource resource = m_resourceDao.getResourceById(id);
        if (resource == null) {
            return null;
        }
        
        PrefabGraph[] graphs = m_graphDao.getPrefabGraphsForResource(resource);
        
        List<String[]> labels = new ArrayList<String[]>(graphs.length);
        for (PrefabGraph graph : graphs) {
            labels.add(new String[] { graph.getName(), graph.getName() });
        }

        return labels.toArray(new String[labels.size()][]);
    }

    public void afterPropertiesSet() throws Exception {
        Assert.state(m_nodeDao != null, "nodeDao property must be set and cannot be null");
        Assert.state(m_resourceDao != null, "resourceDao property must be set and cannot be null");
        Assert.state(m_graphDao != null, "graphDao property must be set and cannot be null");
        Assert.state(m_webSurveillanceService != null, "webSurveillanceService property must be set and cannot be null");
        Assert.state(m_surveillanceViewConfigDao != null, "surveillanceViewConfigDao property must be set and cannot be null");
        Assert.state(m_categoryDao != null, "categoryDao property must be set and cannot be null");
        Assert.state(m_alarmDao != null, "alarmDao property must be set and cannot be null");
    }

    public void setNodeDao(NodeDao nodeDao) {
        m_nodeDao = nodeDao;
    }
    
    public void setResourceDao(ResourceDao resourceDao) {
        m_resourceDao = resourceDao;
    }

    public void setGraphDao(GraphDao graphDao) {
        m_graphDao = graphDao;
    }

    public org.opennms.web.svclayer.SurveillanceService getWebSurveillanceService() {
        return m_webSurveillanceService;
    }

    public void setWebSurveillanceService(org.opennms.web.svclayer.SurveillanceService webSurveillanceService) {
        m_webSurveillanceService = webSurveillanceService;
    }

    public SurveillanceViewConfigDao getSurveillanceViewConfigDao() {
        return m_surveillanceViewConfigDao;
    }

    public void setSurveillanceViewConfigDao(SurveillanceViewConfigDao surveillanceViewConfigDao) {
        m_surveillanceViewConfigDao = surveillanceViewConfigDao;
    }

    public CategoryDao getCategoryDao() {
        return m_categoryDao;
    }

    public void setCategoryDao(CategoryDao categoryDao) {
        m_categoryDao = categoryDao;
    }

    public AlarmDao getAlarmDao() {
        return m_alarmDao;
    }

    public void setAlarmDao(AlarmDao alarmDao) {
        m_alarmDao = alarmDao;
    }

}
