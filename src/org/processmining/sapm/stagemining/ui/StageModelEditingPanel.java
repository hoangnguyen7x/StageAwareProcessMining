package org.processmining.sapm.stagemining.ui;

//https://ateraimemo.com/Swing/DnDTabbedPane.html

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.sapm.stagemining.model.Stage;
import org.processmining.sapm.stagemining.model.StageDecomposition;
import org.processmining.sapm.stagemining.model.StageEditConfig;
import org.processmining.sapm.stagemining.model.StageItem;
import org.processmining.sapm.stagemining.model.StageModelEditConfig;
import org.processmining.sapm.stagemining.vis.StageGraph;

public final class StageModelEditingPanel extends JPanel {
	private StageDecomposition stageModel = null;
	private UIPluginContext context = null;
    private final DnDTabbedPane tab = new DnDTabbedPane();
    private List<StageItem> removedActivities = null;
    public StageModelEditingPanel(UIPluginContext context, StageDecomposition stageModel) {
        super(new BorderLayout());
        this.stageModel = stageModel;
        this.context = context;
        this.removedActivities = new ArrayList<>();
        tab.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        for (Stage stage : stageModel.getStageListModel()) {
        	int stageIndex = stageModel.getStageListModel().indexOf(stage) + 1;
        	tab.addTab("Stage " + stageIndex, new StageConfigPanel(stage, stageModel.getStageListModel(), removedActivities));
        }

        add(tab);
        add(makeButtonPanel(), BorderLayout.SOUTH);
        setPreferredSize(new Dimension(320, 240));
    }
    
    private JComponent makeButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnViewStageModel = new JButton("View Current Stage Model...");
        buttonPanel.add(btnViewStageModel);
        btnViewStageModel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JDialog diaglog = new JDialog();
		        diaglog.setTitle("Stage Model Visualization");
		        diaglog.setModal(false);
		        diaglog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		        JPanel stageModelPanel = new StageGraphPanel(context, new StageGraph(stageModel));
		        stageModelPanel.setOpaque(true); //content panes must be opaque
		        diaglog.setContentPane(stageModelPanel);

		        //Display the window.
		        diaglog.setPreferredSize(new Dimension(600, 1000));
		        diaglog.pack();
		        diaglog.setVisible(true);
			}
		});
        JPanel p = new JPanel(new BorderLayout());
        p.add(buttonPanel, BorderLayout.CENTER);
        return p;
    }
    
    /*
     * Return 
     * -1: there are unallocated removed activities
     * -2: the last stage should set `end' as its milestone
     * -3: there is an intermediary stage that has `end' as milestone node
     * -4: two stages have the same milestone node
     */
    public int checkUserInput() {
    	if (!removedActivities.isEmpty()) return -1;
    	
    	int totalTabs = this.tab.getTabCount();
    	Set<String> milestones = new HashSet<>();
    	
    	for(int i = 0; i < totalTabs; i++) {
    		StageEditConfig stageConfig = ((StageConfigPanel)tab.getComponentAt(i)).getStageModelEditConfig();
    		if (i==totalTabs-1) {
    			if (stageConfig.getEndMilestone().getNode() != this.stageModel.getBaseGraph().getSink()) {
    				return -2;
    			}
    		}
    		else {
    			if (stageConfig.getEndMilestone().getNode() == this.stageModel.getBaseGraph().getSink()) {
    				StageEditConfig nextStageConfig = ((StageConfigPanel)tab.getComponentAt(i+1)).getStageModelEditConfig();
    				if (!nextStageConfig.isMergeWithPrevStage()) {
    					return -3;
    				}
    			}
    		}
    		
    		if (!milestones.contains(stageConfig.getEndMilestone().getLabel())) {
    			milestones.add(stageConfig.getEndMilestone().getLabel());
    		}
    		else {
    			return -4;
    		}
    	}
    	return 0;
    }
    
    public StageModelEditConfig getStageModelEditConfig() {
    	StageModelEditConfig stageModelEditConfig = new StageModelEditConfig();
    	int totalTabs = this.tab.getTabCount();
    	for(int i = 0; i < totalTabs; i++) {
    		stageModelEditConfig.add(((StageConfigPanel)tab.getComponentAt(i)).getStageModelEditConfig());
    	}
    	return stageModelEditConfig;
    }
 
}

class DnDTabbedPane extends JTabbedPane {
    private static final int LINEWIDTH = 3;
    private static final int RWH = 20;
    private static final int BUTTON_SIZE = 30; //XXX 30 is magic number of scroll button size

    private final GhostGlassPane glassPane = new GhostGlassPane(this);
    protected int dragTabIndex = -1;

    //For Debug: >>>
    protected boolean hasGhost = true;
    protected boolean isPaintScrollArea = true;
    //<<<

    protected Rectangle rBackward = new Rectangle();
    protected Rectangle rForward  = new Rectangle();
    public void autoScrollTest(Point glassPt) {
        Rectangle r = getTabAreaBounds();
        if (isTopBottomTabPlacement(getTabPlacement())) {
            rBackward.setBounds(r.x, r.y, RWH, r.height);
            rForward.setBounds(r.x + r.width - RWH - BUTTON_SIZE, r.y, RWH + BUTTON_SIZE, r.height);
        } else {
            rBackward.setBounds(r.x, r.y, r.width, RWH);
            rForward.setBounds(r.x, r.y + r.height - RWH - BUTTON_SIZE, r.width, RWH + BUTTON_SIZE);
        }
        rBackward = SwingUtilities.convertRectangle(getParent(), rBackward, glassPane);
        rForward  = SwingUtilities.convertRectangle(getParent(), rForward,  glassPane);
        if (rBackward.contains(glassPt)) {
            clickArrowButton("scrollTabsBackwardAction");
        } else if (rForward.contains(glassPt)) {
            clickArrowButton("scrollTabsForwardAction");
        }
    }
    private void clickArrowButton(String actionKey) {
        ActionMap am = getActionMap(); //= getActionMap(create=true) = non null
        Optional.ofNullable(am.get(actionKey)).filter(Action::isEnabled).ifPresent(a -> {
            a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null, 0, 0));
        });
//         ActionMap map = getActionMap();
//         if (map != null) {
//             Action action = map.get(actionKey);
//             if (action != null && action.isEnabled()) {
//                 action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null, 0, 0));
//             }
//         }
    }

    protected DnDTabbedPane() {
        super();
        glassPane.setName("GlassPane");
        new DropTarget(glassPane, DnDConstants.ACTION_COPY_OR_MOVE, new TabDropTargetListener(), true);
        DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY_OR_MOVE, new TabDragGestureListener());
    }

    protected int getTargetTabIndex(Point glassPt) {
        Point tabPt = SwingUtilities.convertPoint(glassPane, glassPt, this);
        Point d = isTopBottomTabPlacement(getTabPlacement()) ? new Point(1, 0) : new Point(0, 1);
        return IntStream.range(0, getTabCount()).filter(i -> {
            Rectangle r = getBoundsAt(i);
            r.translate(-r.width * d.x / 2, -r.height * d.y / 2);
            return r.contains(tabPt);
        }).findFirst().orElseGet(() -> {
            int count = getTabCount();
            Rectangle r = getBoundsAt(count - 1);
            r.translate(r.width * d.x / 2, r.height * d.y / 2);
            return r.contains(tabPt) ? count : -1;
        });
//         for (int i = 0; i < getTabCount(); i++) {
//             Rectangle r = getBoundsAt(i);
//             r.translate(-r.width * d.x / 2, -r.height * d.y / 2);
//             if (r.contains(tabPt)) {
//                 return i;
//             }
//         }
//         Rectangle r = getBoundsAt(getTabCount() - 1);
//         r.translate(r.width * d.x / 2, r.height * d.y / 2);
//         return r.contains(tabPt) ? getTabCount() : -1;
    }

    protected void convertTab(int prev, int next) {
//         if (next < 0 || prev == next) {
//             return;
//         }
        Component cmp = getComponentAt(prev);
        Component tab = getTabComponentAt(prev);
        String str    = getTitleAt(prev);
        Icon icon     = getIconAt(prev);
        String tip    = getToolTipTextAt(prev);
        boolean flg   = isEnabledAt(prev);
        int tgtindex  = prev > next ? next : next - 1;
        remove(prev);
        insertTab(str, icon, cmp, tip, tgtindex);
        setEnabledAt(tgtindex, flg);
        //When you drag'n'drop a disabled tab, it finishes enabled and selected.
        //pointed out by dlorde
        if (flg) {
            setSelectedIndex(tgtindex);
        }
        //I have a component in all tabs (jlabel with an X to close the tab) and when i move a tab the component disappear.
        //pointed out by Daniel Dario Morales Salas
        setTabComponentAt(tgtindex, tab);
    }

    protected void initTargetLine(int next) {
        boolean isLeftOrRightNeighbor = next < 0 || dragTabIndex == next || next - dragTabIndex == 1;
        if (isLeftOrRightNeighbor) {
            glassPane.setTargetRect(0, 0, 0, 0);
            return;
        }
        Rectangle r = SwingUtilities.convertRectangle(this, getBoundsAt(Math.max(0, next - 1)), glassPane);
        int a = Math.min(next, 1); //a = (next == 0) ? 0 : 1;
        if (isTopBottomTabPlacement(getTabPlacement())) {
            glassPane.setTargetRect(r.x + r.width * a - LINEWIDTH / 2, r.y, LINEWIDTH, r.height);
        } else {
            glassPane.setTargetRect(r.x, r.y + r.height * a - LINEWIDTH / 2, r.width, LINEWIDTH);
        }
    }

    protected void initGlassPane(Point tabPt) {
        getRootPane().setGlassPane(glassPane);
        if (hasGhost) {
            Rectangle rect = getBoundsAt(dragTabIndex);
            BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = image.createGraphics();
            paint(g2);
            g2.dispose();
            rect.x = Math.max(0, rect.x); //rect.x < 0 ? 0 : rect.x;
            rect.y = Math.max(0, rect.y); //rect.y < 0 ? 0 : rect.y;
            image = image.getSubimage(rect.x, rect.y, rect.width, rect.height);
            glassPane.setImage(image);
        }
        Point glassPt = SwingUtilities.convertPoint(this, tabPt, glassPane);
        glassPane.setPoint(glassPt);
        glassPane.setVisible(true);
    }

    protected Rectangle getTabAreaBounds() {
        Rectangle tabbedRect = getBounds();
        //XXX: Rectangle compRect = getSelectedComponent().getBounds();
        //pointed out by daryl. NullPointerException: i.e. addTab("Tab", null)
        //Component comp = getSelectedComponent();
        //int idx = 0;
        //while (comp == null && idx < getTabCount()) {
        //    comp = getComponentAt(idx++);
        //}

        Rectangle compRect = Optional.ofNullable(getSelectedComponent()).map(Component::getBounds).orElseGet(Rectangle::new);
//         //TEST:
//         Rectangle compRect = Optional.ofNullable(getSelectedComponent())
//                                      .map(Component::getBounds)
//                                      .orElseGet(() -> IntStream.range(0, getTabCount())
//                                                                .mapToObj(this::getComponentAt)
//                                                                .map(Component::getBounds)
//                                                                .findFirst()
//                                                                .orElseGet(Rectangle::new));
        int tabPlacement = getTabPlacement();
        if (isTopBottomTabPlacement(tabPlacement)) {
            tabbedRect.height = tabbedRect.height - compRect.height;
            if (tabPlacement == BOTTOM) {
                tabbedRect.y += compRect.y + compRect.height;
            }
        } else {
            tabbedRect.width = tabbedRect.width - compRect.width;
            if (tabPlacement == RIGHT) {
                tabbedRect.x += compRect.x + compRect.width;
            }
        }
//         if (tabPlacement == TOP) {
//             tabbedRect.height = tabbedRect.height - compRect.height;
//         } else if (tabPlacement == BOTTOM) {
//             tabbedRect.y = tabbedRect.y + compRect.y + compRect.height;
//             tabbedRect.height = tabbedRect.height - compRect.height;
//         } else if (tabPlacement == LEFT) {
//             tabbedRect.width = tabbedRect.width - compRect.width;
//         } else if (tabPlacement == RIGHT) {
//             tabbedRect.x = tabbedRect.x + compRect.x + compRect.width;
//             tabbedRect.width = tabbedRect.width - compRect.width;
//         }
        tabbedRect.grow(2, 2);
        return tabbedRect;
    }
    public static boolean isTopBottomTabPlacement(int tabPlacement) {
        return tabPlacement == JTabbedPane.TOP || tabPlacement == JTabbedPane.BOTTOM;
    }
}

class TabTransferable implements Transferable {
    private static final String NAME = "test";
    private static final DataFlavor FLAVOR = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType, NAME);
    private final Component tabbedPane;
    protected TabTransferable(Component tabbedPane) {
        this.tabbedPane = tabbedPane;
    }
    @Override public Object getTransferData(DataFlavor flavor) {
        return tabbedPane;
    }
    @Override public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[] {FLAVOR};
    }
    @Override public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.getHumanPresentableName().equals(NAME);
    }
}

class TabDragSourceListener implements DragSourceListener {
    @Override public void dragEnter(DragSourceDragEvent e) {
        e.getDragSourceContext().setCursor(DragSource.DefaultMoveDrop);
    }
    @Override public void dragExit(DragSourceEvent e) {
        e.getDragSourceContext().setCursor(DragSource.DefaultMoveNoDrop);
        //glassPane.setTargetRect(0, 0, 0, 0);
        //glassPane.setPoint(new Point(-1000, -1000));
        //glassPane.repaint();
    }
    @Override public void dragOver(DragSourceDragEvent e) {
        //Point glassPt = e.getLocation();
        //JComponent glassPane = (JComponent) e.getDragSourceContext();
        //SwingUtilities.convertPointFromScreen(glassPt, glassPane);
        //int targetIdx = getTargetTabIndex(glassPt);
        //if (getTabAreaBounds().contains(glassPt) && targetIdx >= 0 && targetIdx != dragTabIndex && targetIdx != dragTabIndex + 1) {
        //    e.getDragSourceContext().setCursor(DragSource.DefaultMoveDrop);
        //    glassPane.setCursor(DragSource.DefaultMoveDrop);
        //} else {
        //    e.getDragSourceContext().setCursor(DragSource.DefaultMoveNoDrop);
        //    glassPane.setCursor(DragSource.DefaultMoveNoDrop);
        //}
    }
    @Override public void dragDropEnd(DragSourceDropEvent e) {
        //dragTabIndex = -1;
        //glassPane.setVisible(false);
    }
    @Override public void dropActionChanged(DragSourceDragEvent e) { /* not needed */ }
}

class TabDragGestureListener implements DragGestureListener {
    @Override public void dragGestureRecognized(DragGestureEvent e) {
        Optional.ofNullable(e.getComponent())
            .filter(c -> c instanceof DnDTabbedPane).map(c -> (DnDTabbedPane) c)
            .filter(tabbedPane -> tabbedPane.getTabCount() > 1)
            .ifPresent(tabbedPane -> {
                Point tabPt = e.getDragOrigin();
                tabbedPane.dragTabIndex = tabbedPane.indexAtLocation(tabPt.x, tabPt.y);
                if (tabbedPane.dragTabIndex >= 0 && tabbedPane.isEnabledAt(tabbedPane.dragTabIndex)) {
                    tabbedPane.initGlassPane(tabPt);
                    try {
                        e.startDrag(DragSource.DefaultMoveDrop, new TabTransferable(tabbedPane), new TabDragSourceListener());
                    } catch (InvalidDnDOperationException ex) {
                        ex.printStackTrace();
                    }
                }
            });
    }
}

class TabDropTargetListener implements DropTargetListener {
    private static final Point HIDDEN_POINT = new Point(0, -1000);
    private static Optional<GhostGlassPane> getGhostGlassPane(Component component) {
        return Optional.ofNullable(component).filter(c -> c instanceof GhostGlassPane).map(c -> (GhostGlassPane) c);
    }
    @Override public void dragEnter(DropTargetDragEvent e) {
        getGhostGlassPane(e.getDropTargetContext().getComponent()).ifPresent(glassPane -> {
            //DnDTabbedPane tabbedPane = glassPane.tabbedPane;
            Transferable t = e.getTransferable();
            DataFlavor[] f = e.getCurrentDataFlavors();
            if (t.isDataFlavorSupported(f[0])) { // && tabbedPane.dragTabIndex >= 0) {
                e.acceptDrag(e.getDropAction());
            } else {
                e.rejectDrag();
            }
        });
    }
    @Override public void dragExit(DropTargetEvent e) {
        //Component c = e.getDropTargetContext().getComponent();
        //System.out.println("DropTargetListener#dragExit: " + c.getName());
        getGhostGlassPane(e.getDropTargetContext().getComponent()).ifPresent(glassPane -> {
            //XXX: glassPane.setVisible(false);
            glassPane.setPoint(HIDDEN_POINT);
            glassPane.setTargetRect(0, 0, 0, 0);
            glassPane.repaint();
        });
    }
    @Override public void dropActionChanged(DropTargetDragEvent e) { /* not needed */ }
    @Override public void dragOver(DropTargetDragEvent e) {
        Component c = e.getDropTargetContext().getComponent();
        getGhostGlassPane(c).ifPresent(glassPane -> {
            Point glassPt = e.getLocation();

            DnDTabbedPane tabbedPane = glassPane.tabbedPane;
            tabbedPane.initTargetLine(tabbedPane.getTargetTabIndex(glassPt));
            tabbedPane.autoScrollTest(glassPt);

            glassPane.setPoint(glassPt);
            glassPane.repaint();
        });
    }
    @Override public void drop(DropTargetDropEvent e) {
        Component c = e.getDropTargetContext().getComponent();
        getGhostGlassPane(c).ifPresent(glassPane -> {
            DnDTabbedPane tabbedPane = glassPane.tabbedPane;
            Transferable t = e.getTransferable();
            DataFlavor[] f = t.getTransferDataFlavors();
            int prev = tabbedPane.dragTabIndex;
            int next = tabbedPane.getTargetTabIndex(e.getLocation());
            if (t.isDataFlavorSupported(f[0]) && prev != next) {
                tabbedPane.convertTab(prev, next);
                e.dropComplete(true);
            } else {
                e.dropComplete(false);
            }
            glassPane.setVisible(false);
            //tabbedPane.dragTabIndex = -1;
        });
    }
}

class GhostGlassPane extends JPanel {
    private static final AlphaComposite ALPHA = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .5f);
    public final DnDTabbedPane tabbedPane;
    private final Rectangle lineRect  = new Rectangle();
    private final Color     lineColor = new Color(0, 100, 255);
    private final Point     location  = new Point();
    private transient Optional<BufferedImage> draggingGhostOp;

    protected GhostGlassPane(DnDTabbedPane tabbedPane) {
        super();
        this.tabbedPane = tabbedPane;
        setOpaque(false);
        // [JDK-6700748] Cursor flickering during D&D when using CellRendererPane with validation - Java Bug System
        // https://bugs.openjdk.java.net/browse/JDK-6700748
        // setCursor(null);
    }
    public void setTargetRect(int x, int y, int width, int height) {
        lineRect.setBounds(x, y, width, height);
    }
    public void setImage(BufferedImage draggingGhost) {
        this.draggingGhostOp = Optional.ofNullable(draggingGhost);
    }
    public void setPoint(Point location) {
        this.location.setLocation(location);
    }
    @Override public void setVisible(boolean v) {
        super.setVisible(v);
        if (!v) {
            setTargetRect(0, 0, 0, 0);
            setImage(null);
        }
    }
    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(ALPHA);
        if (tabbedPane.isPaintScrollArea && tabbedPane.getTabLayoutPolicy() == JTabbedPane.SCROLL_TAB_LAYOUT) {
            g2.setPaint(Color.RED);
            g2.fill(tabbedPane.rBackward);
            g2.fill(tabbedPane.rForward);
        }
        draggingGhostOp.ifPresent(img -> {
            double xx = location.getX() - img.getWidth(this)  / 2d;
            double yy = location.getY() - img.getHeight(this) / 2d;
            g2.drawImage(img, (int) xx, (int) yy, null);
        });
        g2.setPaint(lineColor);
        g2.fill(lineRect);
        g2.dispose();
    }
}
