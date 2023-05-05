
import java.awt.Color;
import java.util.List;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;


/**
 *
 * @author jacka
 */



public class UserGUI extends javax.swing.JFrame {
    private DBCon database;
    private final String PlaceholderText = ("Enter Value Here...");
    private String CommonTaskName;
    private Integer CommonTaskUser1Est;
    private Integer CommonTaskUser2Est;
    private Integer User1Est;
    private Integer User2Est;
    private String NewCommonTaskName;
    private Integer NewCommonTaskUser1Est;
    private Integer NewCommonTaskUser2Est;
    private String OneOffTaskName;
    private Integer OneOffUser1Est;
    private Integer OneOffUser2Est;
    private String RemoveSelectedTask;
    private Boolean TaskSelected;
    private ArrayList<String>AllAvailableCommonTasks;
    private ArrayList<String>AllTasks;
    private ArrayList<String>AllSelectedTasks;
    private Map<String, List<Integer>> SelectedTasks;
    private DefaultTableModel model;
 
    /**
     * Creates new form UserGUI
     */
    public UserGUI() {
        ConnectToDatabase();
        GetAllTasks();
        RefreshAllAvailableCommonTasks();
        GetAllSelectedTasks();
        initComponents();
        FillTable();
        

        HomePage.setVisible(true);
        AddTaskPage.setVisible(false);
        AddNewCommonTask.setVisible(false);
        SubmitConfirmationDialog.setVisible(false);
        SubmissionFailedDialog.setVisible(false);
        RemoveTaskDialog.setVisible(false);

    }
    
    public void ConnectToDatabase(){
        database = new DBCon();
        database.Connect("C:\\Users\\jacka\\OneDrive\\Documents\\Software Engineering Practice\\DB\\AssessmentDB");
    }
    
    public ArrayList GetAllTasks(){
        if(AllTasks != null){
            AllTasks.clear();
        }
        String sqlString = new String ("SELECT task_name FROM commonTasks "
                + "UNION SELECT task_name FROM oneOffTasks;");
        ResultSet EveryTask = database.RunSQLQuery(sqlString);
        AllTasks = new ArrayList<String>();
        
        try{
            while(EveryTask.next()){
                String TaskName = EveryTask.getString(1);
                AllTasks.add(TaskName);
            }
        }
        catch (SQLException e){
            System.out.println("Failed to process query in GetCommonTasks()");
            System.out.println("SQL attempted: "+sqlString);	
            System.out.println("Error: "+e.getErrorCode());
            System.out.println("Message: "+e.getMessage());			
            e.printStackTrace();
        }
        return AllTasks;
    }
    
    public void RefreshAllAvailableCommonTasks(){
        String sqlString = new String ("SELECT task_name FROM commonTasks WHERE selected = false;");
        ResultSet CommonTasks = database.RunSQLQuery(sqlString);
        AllAvailableCommonTasks = new ArrayList<>();

        try{
            while(CommonTasks.next()){
                CommonTaskName = CommonTasks.getString(1);
                AllAvailableCommonTasks.add(CommonTaskName);
            }
        }
        catch (SQLException e){
            System.out.println("Failed to process query in GetAllCommonTasks()");
            System.out.println("SQL attempted: "+sqlString);	
            System.out.println("Error: "+e.getErrorCode());
            System.out.println("Message: "+e.getMessage());			
            e.printStackTrace();   
            
        }
    }
    
    public void FillTable(){
        try{
            String sqlString = new String("SELECT task_name, user1_estTime, user2_estTime, assigned_user, act_time "
                                            + "FROM commonTasks WHERE selected = true "
                                            + "UNION SELECT task_name, user1_estTime, user2_estTime, assigned_user, act_time "
                                            + "FROM oneOffTasks");
            ResultSet TableData = database.RunSQLQuery(sqlString);
            ResultSetMetaData TableMetaData = TableData.getMetaData();
            model = (DefaultTableModel)TaskTable.getModel();
            model.setRowCount(0);
            
            int columns = TableMetaData.getColumnCount();
            String[]ColumnName = new String[columns];
            for (int i = 0; i < columns; i++){
                ColumnName[i] = TableMetaData.getColumnName(i+1);
            }
            model.setColumnIdentifiers(ColumnName);
            String TaskName, AssignedUser,User1EstTime, User2EstTime, ActTime;
            while(TableData.next()){
                TaskName = TableData.getString(1);
                User1EstTime = TableData.getString(2);
                User2EstTime = TableData.getString(3);
                AssignedUser = TableData.getString(3);
                ActTime = TableData.getString(4);
                String[] row = {TaskName,User1EstTime, User2EstTime, ActTime};
                model.addRow(row);
            }
            
        }
        catch(SQLException ex){
        }
    }
    
        public void AssignTasksAlgo(){
        Integer User1Tot = 0;
        Integer User2Tot = 0;
        float User1Load = 0;
        float User2Load = 0;
        HashMap <String,Float> User1hmp = new HashMap<String,Float>();
        HashMap <String,Float> User2hmp = new HashMap<String,Float>();
        for(int i = 0; i < TaskTable.getRowCount(); i++){
            User1Tot += Integer.parseInt(TaskTable.getValueAt(i, 1).toString());
            User2Tot += Integer.parseInt(TaskTable.getValueAt(i,2).toString());
        }
        String sqlString = new String ("SELECT weekly_load FROM users WHERE user_id = 1; ");
        ResultSet U1Loadrs = database.RunSQLQuery(sqlString);
        try{
        User1Load = U1Loadrs.getFloat(1);
        }
        catch(SQLException ex){
        }
        String sqlString1 = new String ("SELECT weekly_load FROM users WHERE user_id = 2; ");
        ResultSet u2Loadrs = database.RunSQLQuery(sqlString1);
        try{
            User2Load = u2Loadrs.getFloat(1);
        }
        catch(SQLException ex){}
        for (int i = 0; i < TaskTable.getRowCount(); i++){
            User1hmp.put(TaskTable.getValueAt(i,0).toString(), Float.parseFloat(TaskTable.getValueAt(i,1).toString())/User1Tot);
            User2hmp.put(TaskTable.getValueAt(i,0).toString(), Float.parseFloat(TaskTable.getValueAt(i,2).toString())/User2Tot);
        }
        LinkedHashMap <String,Float> SortedUser1hmp = User1hmp
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(
                        Map.Entry :: getKey, 
                        Map.Entry :: getValue, 
                        (oldValue,newValue) -> oldValue,
                        LinkedHashMap::new));
        
        LinkedHashMap <String,Float> SortedUser2hmp = User2hmp
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(
                        Map.Entry :: getKey,
                        Map.Entry :: getValue,
                        (oldValue,newValue) -> oldValue,
                        LinkedHashMap::new));
        
                
        for (int i = 0; i < TaskTable.getRowCount(); i++){
            Map.Entry <String, Float> U1KeyEntry = SortedUser1hmp.entrySet().iterator().next();
            Map.Entry <String, Float> U2KeyEntry = SortedUser2hmp.entrySet().iterator().next();
            String U1Key =  U1KeyEntry.getKey();
            Float U1Value = U1KeyEntry.getValue();
            String U2Key = U2KeyEntry.getKey();
            Float U2Value = U2KeyEntry.getValue();
            
            if(User1Load < User2Load){
                String assignU1 = new String ("UPDATE commonTasks SET assigned_user = 1"
                                                   + " WHERE task_name = ('"+U1Key+"');");
            
                    boolean success = database.RunSQL(assignU1);
                    if(!success){
                        System.out.println("Cannot Run");
                }
                String assignU1OneOff = new String("UPDATE oneOffTasks SET assigned_user = 1"
                                                    + " WHERE task_name = ('"+U1Key+"');"); 
                boolean success1 = database.RunSQL(assignU1OneOff);
                if(!success1){
                    System.out.println("Cannot Run");
                }
                User1Load += U1Value;
                SortedUser1hmp.remove(U1Key);
                SortedUser2hmp.remove(U2Key);
            }
            
            else if(User2Load < User1Load){
                String assignU2 = new String ("UPDATE commonTasks SET assigned_user = 2"
                                                   +" WHERE task_name = ('"+U2Key+"');");
                                                   
                    boolean success = database.RunSQL(assignU2);
                    if(!success){
                        System.out.println("Cannot Run");
                    }
                    
                String assignU2OneOff = new String ("UPDATE oneOffTasks SET assigned_user = 2"
                                                   +" WHERE task_name = ('"+U2Key+"');");
                boolean success1 = database.RunSQL(assignU2OneOff);
                if (!success1){
                    System.out.println("Cannot Run");
                }
                    User2Load += U2Value;
                    SortedUser1hmp.remove(U1Key);
                    SortedUser2hmp.remove(U2Key);
                }
          
            
            
            
            else if(User1Load == User2Load){
                if(U1Value <= U2Value){
                    String assignUser1 = new String ("UPDATE commonTasks SET assigned_user = 1"
                                                   + " WHERE task_name = ('"+U1Key+"');");
                                                
                                                  
                    boolean success = database.RunSQL(assignUser1);
                    if(!success){
                        System.out.println("Cannot Run");
                    }
                    
                    String assignUser1OneOff = new String ("UPDATE oneOffTasks SET assigned_user = 1"
                                                   + " WHERE task_name = ('"+U1Key+"');");
                    boolean success1 = database.RunSQL(assignUser1OneOff);
                    if(!success1){
                        System.out.println("Cannot Run");
                    }
                    User1Load = User1Load + U1Value;
                    SortedUser1hmp.remove(U1Key);
                    SortedUser2hmp.remove(U2Key);
                }
                else{
                    String assignUser2 = new String ("UPDATE commonTasks SET assigned_user = 2"
                                               + " WHERE task_name = ('"+U2Key+"');");
                                             
                                               
                    boolean success = database.RunSQL(assignUser2);
                    if(!success){
                        System.out.println("Cannot Run"); 
                    }
                    
                    String assignUser2OneOff = new String ("UPDATE oneOffTasks SET assigned_user = 2"
                                               + " WHERE task_name = ('"+U2Key+"');");
                    boolean success1 = database.RunSQL(assignUser2OneOff);
                    if(!success1){
                        System.out.println("Cannot Run");
                    }
                    User2Load += U2Value;
                    SortedUser1hmp.remove(U1Key);
                    SortedUser2hmp.remove(U2Key);
                }
            }
        
        }
        String UpdateUser1Data = new String ("UPDATE users SET weekly_load = "+User1Load+" WHERE user_id = 1;");
        boolean success = database.RunSQL(UpdateUser1Data);
        if(!success){
            System.out.println("Cannot Run");
        }
        String UpdateUser2Data = new String ("UPDATE users SET weekly_load = "+User2Load+" WHERE user_id = 2;");
        boolean success1 = database.RunSQL(UpdateUser2Data);
        if(!success1){
            System.out.println("Cannot Run");
        }
        
        FillTable();
        System.out.println(User1Load);
        System.out.println(User2Load);
    }
    
    
    public void GetAllSelectedTasks(){
         String sqlString = new String ("SELECT task_name FROM commonTasks WHERE selected = true "
                 + "UNION SELECT task_name FROM oneOffTasks;");
         ResultSet AllSelectedTaskSet = database.RunSQLQuery(sqlString);
         AllSelectedTasks = new ArrayList<String>();
         
         try{
             while(AllSelectedTaskSet.next()){
                 String SelectedTaskName = AllSelectedTaskSet.getString(1);
                 AllSelectedTasks.add(SelectedTaskName);
             }
         }
         catch (SQLException e){
            System.out.println("Failed to process query in GetAllCommonTasks()");
            System.out.println("SQL attempted: "+sqlString);	
            System.out.println("Error: "+e.getErrorCode());
            System.out.println("Message: "+e.getMessage());			
            e.printStackTrace();  
        }
    }
    
    
    

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        AddNewCommonTask = new javax.swing.JDialog();
        NewCommonTaskNameLabel = new javax.swing.JLabel();
        NewCommonTaskNameInput = new javax.swing.JTextField();
        NewCommonTaskUser1EstLabel = new javax.swing.JLabel();
        NewCommonTaskUser2EstLabel = new javax.swing.JLabel();
        NewCommonTaskUser1EstInput = new javax.swing.JTextField();
        NewCommonTaskUser2EstInput = new javax.swing.JTextField();
        NewCommonSubmitButton = new javax.swing.JButton();
        NewCommonCancelButton = new javax.swing.JButton();
        SubmitConfirmationDialog = new javax.swing.JDialog();
        SubmissionConfirmationLabel = new javax.swing.JLabel();
        SubmissionConfirmationOkayButton = new javax.swing.JButton();
        SubmissionFailedDialog = new javax.swing.JDialog();
        SubmissionFailedLabel = new javax.swing.JLabel();
        SubmissionFailedOkayButton = new javax.swing.JButton();
        RemoveTaskDialog = new javax.swing.JDialog();
        RemoveTaskLabel = new javax.swing.JLabel();
        String [] SelectedTasks = AllSelectedTasks.toArray(new String[AllSelectedTasks.size()]);
        RemoveTaskList = new javax.swing.JComboBox<>(SelectedTasks);
        RemoveTaskConfirmButton = new javax.swing.JButton();
        RemoveTaskCancelButton = new javax.swing.JButton();
        jLayeredPane2 = new javax.swing.JLayeredPane();
        HomePage = new javax.swing.JPanel();
        Title = new javax.swing.JLabel();
        AddTaskButton = new javax.swing.JButton();
        RemoveTaskButton = new javax.swing.JButton();
        AssignTaskButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        TaskTable = new javax.swing.JTable();
        AddTaskPage = new javax.swing.JPanel();
        CommonTaskSubtitle = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        AddNewCommonTaskButton = new javax.swing.JButton();
        OneOffTaskSubtitle = new javax.swing.JLabel();
        User1CommonTaskEstInput = new javax.swing.JTextField();
        User1CommonTaskEstLabel = new javax.swing.JLabel();
        User2CommonTaskEstLabel = new javax.swing.JLabel();
        User2CommonTaskEstInput = new javax.swing.JTextField();
        OneOffNameLabel = new javax.swing.JLabel();
        OneOffNameInput = new javax.swing.JTextField();
        User1OneOffEstLabel = new javax.swing.JLabel();
        User1OneOffEstInput = new javax.swing.JTextField();
        User2OneOffEstLabel = new javax.swing.JLabel();
        User2OneOffEstInput = new javax.swing.JTextField();
        ConfirmButton = new javax.swing.JButton();
        CancelButton = new javax.swing.JButton();
        String [] AvailableTasks = AllAvailableCommonTasks.toArray(new String[AllAvailableCommonTasks.size()]);
        CommonTaskDropDown = new javax.swing.JComboBox<>(AvailableTasks);

        AddNewCommonTask.setMinimumSize(new java.awt.Dimension(400, 400));

        NewCommonTaskNameLabel.setText("Please enter the new tasks name");

        NewCommonTaskNameInput.setForeground(new java.awt.Color(153, 153, 153));
        NewCommonTaskNameInput.setText(PlaceholderText);
        NewCommonTaskNameInput.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                NewCommonTaskNameInputFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                NewCommonTaskNameInputFocusLost(evt);
            }
        });
        NewCommonTaskNameInput.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                NewCommonTaskNameInputActionPerformed(evt);
            }
        });

        NewCommonTaskUser1EstLabel.setText("Please enter User 1 estimated completion time(mins)");

        NewCommonTaskUser2EstLabel.setText("Please enter User 2 estimated completion time(mins)");

        NewCommonTaskUser1EstInput.setForeground(new java.awt.Color(153, 153, 153));
        NewCommonTaskUser1EstInput.setText(PlaceholderText);
        NewCommonTaskUser1EstInput.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                NewCommonTaskUser1EstInputFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                NewCommonTaskUser1EstInputFocusLost(evt);
            }
        });
        NewCommonTaskUser1EstInput.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                NewCommonTaskUser1EstInputActionPerformed(evt);
            }
        });

        NewCommonTaskUser2EstInput.setForeground(new java.awt.Color(153, 153, 153));
        NewCommonTaskUser2EstInput.setText(PlaceholderText);
        NewCommonTaskUser2EstInput.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                NewCommonTaskUser2EstInputFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                NewCommonTaskUser2EstInputFocusLost(evt);
            }
        });
        NewCommonTaskUser2EstInput.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                NewCommonTaskUser2EstInputActionPerformed(evt);
            }
        });

        NewCommonSubmitButton.setText("Submit");
        NewCommonSubmitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                NewCommonSubmitButtonActionPerformed(evt);
            }
        });

        NewCommonCancelButton.setText("Cancel");
        NewCommonCancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                NewCommonCancelButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout AddNewCommonTaskLayout = new javax.swing.GroupLayout(AddNewCommonTask.getContentPane());
        AddNewCommonTask.getContentPane().setLayout(AddNewCommonTaskLayout);
        AddNewCommonTaskLayout.setHorizontalGroup(
            AddNewCommonTaskLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(AddNewCommonTaskLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(AddNewCommonTaskLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(NewCommonTaskNameLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 183, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(NewCommonTaskNameInput, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(NewCommonTaskUser1EstLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 290, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(NewCommonTaskUser1EstInput, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(AddNewCommonTaskLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, AddNewCommonTaskLayout.createSequentialGroup()
                            .addGroup(AddNewCommonTaskLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(NewCommonSubmitButton)
                                .addComponent(NewCommonTaskUser2EstInput, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(NewCommonCancelButton))
                        .addComponent(NewCommonTaskUser2EstLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 290, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(198, Short.MAX_VALUE))
        );
        AddNewCommonTaskLayout.setVerticalGroup(
            AddNewCommonTaskLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(AddNewCommonTaskLayout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(NewCommonTaskNameLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(NewCommonTaskNameInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(NewCommonTaskUser1EstLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(NewCommonTaskUser1EstInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(8, 8, 8)
                .addComponent(NewCommonTaskUser2EstLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(NewCommonTaskUser2EstInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(50, 50, 50)
                .addGroup(AddNewCommonTaskLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(NewCommonSubmitButton)
                    .addComponent(NewCommonCancelButton))
                .addContainerGap(155, Short.MAX_VALUE))
        );

        SubmitConfirmationDialog.setMinimumSize(new java.awt.Dimension(400, 150));

        SubmissionConfirmationLabel.setText("Submission Success, Task has been added to the table");

        SubmissionConfirmationOkayButton.setText("Okay");
        SubmissionConfirmationOkayButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SubmissionConfirmationOkayButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout SubmitConfirmationDialogLayout = new javax.swing.GroupLayout(SubmitConfirmationDialog.getContentPane());
        SubmitConfirmationDialog.getContentPane().setLayout(SubmitConfirmationDialogLayout);
        SubmitConfirmationDialogLayout.setHorizontalGroup(
            SubmitConfirmationDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(SubmitConfirmationDialogLayout.createSequentialGroup()
                .addGroup(SubmitConfirmationDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(SubmitConfirmationDialogLayout.createSequentialGroup()
                        .addGap(69, 69, 69)
                        .addComponent(SubmissionConfirmationLabel))
                    .addGroup(SubmitConfirmationDialogLayout.createSequentialGroup()
                        .addGap(161, 161, 161)
                        .addComponent(SubmissionConfirmationOkayButton)))
                .addContainerGap(46, Short.MAX_VALUE))
        );
        SubmitConfirmationDialogLayout.setVerticalGroup(
            SubmitConfirmationDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(SubmitConfirmationDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(SubmissionConfirmationLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(SubmissionConfirmationOkayButton)
                .addContainerGap(16, Short.MAX_VALUE))
        );

        SubmissionFailedDialog.setMinimumSize(new java.awt.Dimension(400, 150));

        SubmissionFailedLabel.setText("Submission Failed please try again");

        SubmissionFailedOkayButton.setText("Okay");
        SubmissionFailedOkayButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SubmissionFailedOkayButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout SubmissionFailedDialogLayout = new javax.swing.GroupLayout(SubmissionFailedDialog.getContentPane());
        SubmissionFailedDialog.getContentPane().setLayout(SubmissionFailedDialogLayout);
        SubmissionFailedDialogLayout.setHorizontalGroup(
            SubmissionFailedDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, SubmissionFailedDialogLayout.createSequentialGroup()
                .addContainerGap(103, Short.MAX_VALUE)
                .addComponent(SubmissionFailedLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 201, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(96, 96, 96))
            .addGroup(SubmissionFailedDialogLayout.createSequentialGroup()
                .addGap(161, 161, 161)
                .addComponent(SubmissionFailedOkayButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        SubmissionFailedDialogLayout.setVerticalGroup(
            SubmissionFailedDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(SubmissionFailedDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(SubmissionFailedLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(SubmissionFailedOkayButton)
                .addContainerGap(14, Short.MAX_VALUE))
        );

        RemoveTaskDialog.setMinimumSize(new java.awt.Dimension(400, 200));

        RemoveTaskLabel.setText("Please select the task you would like to remove");

        RemoveTaskList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RemoveTaskListActionPerformed(evt);
            }
        });

        RemoveTaskConfirmButton.setText("Confirm");
        RemoveTaskConfirmButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RemoveTaskConfirmButtonActionPerformed(evt);
            }
        });

        RemoveTaskCancelButton.setText("Cancel");
        RemoveTaskCancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RemoveTaskCancelButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout RemoveTaskDialogLayout = new javax.swing.GroupLayout(RemoveTaskDialog.getContentPane());
        RemoveTaskDialog.getContentPane().setLayout(RemoveTaskDialogLayout);
        RemoveTaskDialogLayout.setHorizontalGroup(
            RemoveTaskDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(RemoveTaskDialogLayout.createSequentialGroup()
                .addGroup(RemoveTaskDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(RemoveTaskDialogLayout.createSequentialGroup()
                        .addGap(63, 63, 63)
                        .addComponent(RemoveTaskLabel))
                    .addGroup(RemoveTaskDialogLayout.createSequentialGroup()
                        .addGap(110, 110, 110)
                        .addComponent(RemoveTaskConfirmButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(RemoveTaskCancelButton))
                    .addGroup(RemoveTaskDialogLayout.createSequentialGroup()
                        .addGap(125, 125, 125)
                        .addComponent(RemoveTaskList, javax.swing.GroupLayout.PREFERRED_SIZE, 116, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(88, Short.MAX_VALUE))
        );
        RemoveTaskDialogLayout.setVerticalGroup(
            RemoveTaskDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(RemoveTaskDialogLayout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(RemoveTaskLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(RemoveTaskList, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addGroup(RemoveTaskDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(RemoveTaskConfirmButton)
                    .addComponent(RemoveTaskCancelButton))
                .addContainerGap(91, Short.MAX_VALUE))
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        Title.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        Title.setText("Your Weekly Tasks");

        AddTaskButton.setText("Add");
        AddTaskButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AddTaskButtonActionPerformed(evt);
            }
        });

        RemoveTaskButton.setText("Remove");
        RemoveTaskButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RemoveTaskButtonActionPerformed(evt);
            }
        });

        AssignTaskButton.setText("Assign Tasks");
        AssignTaskButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AssignTaskButtonActionPerformed(evt);
            }
        });

        TaskTable.setModel(new javax.swing.table.DefaultTableModel()
        );
        jScrollPane1.setViewportView(TaskTable);

        javax.swing.GroupLayout HomePageLayout = new javax.swing.GroupLayout(HomePage);
        HomePage.setLayout(HomePageLayout);
        HomePageLayout.setHorizontalGroup(
            HomePageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(HomePageLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(HomePageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, HomePageLayout.createSequentialGroup()
                        .addComponent(AssignTaskButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(AddTaskButton)
                        .addGap(18, 18, 18)
                        .addComponent(RemoveTaskButton))
                    .addGroup(HomePageLayout.createSequentialGroup()
                        .addGap(0, 213, Short.MAX_VALUE)
                        .addComponent(Title, javax.swing.GroupLayout.PREFERRED_SIZE, 364, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        HomePageLayout.setVerticalGroup(
            HomePageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(HomePageLayout.createSequentialGroup()
                .addGap(54, 54, 54)
                .addComponent(Title)
                .addGap(24, 24, 24)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(HomePageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(AddTaskButton)
                    .addComponent(RemoveTaskButton)
                    .addComponent(AssignTaskButton))
                .addContainerGap(56, Short.MAX_VALUE))
        );

        CommonTaskSubtitle.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        CommonTaskSubtitle.setText("Adding a Common Task? Select one from this list and complete the form.");

        jLabel2.setText("Task not there? Click the button below to add it");

        AddNewCommonTaskButton.setText("Click Me");
        AddNewCommonTaskButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AddNewCommonTaskButtonActionPerformed(evt);
            }
        });

        OneOffTaskSubtitle.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        OneOffTaskSubtitle.setText("Adding a One-Off Task? complete the form below to add it to the table");

        User1CommonTaskEstInput.setForeground(new java.awt.Color(153, 153, 153));
        User1CommonTaskEstInput.setText(PlaceholderText);
        User1CommonTaskEstInput.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                User1CommonTaskEstInputFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                User1CommonTaskEstInputFocusLost(evt);
            }
        });

        User1CommonTaskEstLabel.setText("User 1 estimated completion time(Mins)");

        User2CommonTaskEstLabel.setText("User 2 estimated completion time(Mins)");

        User2CommonTaskEstInput.setForeground(new java.awt.Color(153, 153, 153));
        User2CommonTaskEstInput.setText(PlaceholderText);
        User2CommonTaskEstInput.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                User2CommonTaskEstInputFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                User2CommonTaskEstInputFocusLost(evt);
            }
        });

        OneOffNameLabel.setText("Please enter the name of the task");

        OneOffNameInput.setForeground(new java.awt.Color(153, 153, 153));
        OneOffNameInput.setText(PlaceholderText);
        OneOffNameInput.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                OneOffNameInputFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                OneOffNameInputFocusLost(evt);
            }
        });
        OneOffNameInput.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                OneOffNameInputActionPerformed(evt);
            }
        });

        User1OneOffEstLabel.setText("User 1 estimated completion time(Mins)");

        User1OneOffEstInput.setForeground(new java.awt.Color(153, 153, 153));
        User1OneOffEstInput.setText(PlaceholderText);
        User1OneOffEstInput.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                User1OneOffEstInputFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                User1OneOffEstInputFocusLost(evt);
            }
        });
        User1OneOffEstInput.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                User1OneOffEstInputActionPerformed(evt);
            }
        });

        User2OneOffEstLabel.setText("User 2 estimated completion time(Mins)");

        User2OneOffEstInput.setForeground(new java.awt.Color(153, 153, 153));
        User2OneOffEstInput.setText(PlaceholderText);
        User2OneOffEstInput.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                User2OneOffEstInputFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                User2OneOffEstInputFocusLost(evt);
            }
        });

        ConfirmButton.setText("Confirm");
        ConfirmButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ConfirmButtonActionPerformed(evt);
            }
        });

        CancelButton.setText("Cancel");
        CancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CancelButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout AddTaskPageLayout = new javax.swing.GroupLayout(AddTaskPage);
        AddTaskPage.setLayout(AddTaskPageLayout);
        AddTaskPageLayout.setHorizontalGroup(
            AddTaskPageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, AddTaskPageLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(ConfirmButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(CancelButton)
                .addGap(25, 25, 25))
            .addGroup(AddTaskPageLayout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addGroup(AddTaskPageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, AddTaskPageLayout.createSequentialGroup()
                        .addComponent(User2CommonTaskEstLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(206, 206, 206))
                    .addGroup(AddTaskPageLayout.createSequentialGroup()
                        .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(313, 313, 313))
                    .addGroup(AddTaskPageLayout.createSequentialGroup()
                        .addGroup(AddTaskPageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(CommonTaskSubtitle)
                            .addComponent(User1CommonTaskEstLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 235, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(AddTaskPageLayout.createSequentialGroup()
                        .addGroup(AddTaskPageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(AddNewCommonTaskButton)
                            .addComponent(User2OneOffEstInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(User1OneOffEstInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(User1OneOffEstLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 235, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(User2CommonTaskEstInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(User1CommonTaskEstInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(OneOffNameLabel)
                            .addComponent(OneOffNameInput, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(OneOffTaskSubtitle, javax.swing.GroupLayout.PREFERRED_SIZE, 410, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(User2OneOffEstLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 235, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(CommonTaskDropDown, javax.swing.GroupLayout.PREFERRED_SIZE, 155, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        AddTaskPageLayout.setVerticalGroup(
            AddTaskPageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(AddTaskPageLayout.createSequentialGroup()
                .addGap(60, 60, 60)
                .addComponent(CommonTaskSubtitle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(CommonTaskDropDown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(User1CommonTaskEstLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(User1CommonTaskEstInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(User2CommonTaskEstLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(User2CommonTaskEstInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(AddNewCommonTaskButton)
                .addGap(35, 35, 35)
                .addComponent(OneOffTaskSubtitle)
                .addGap(24, 24, 24)
                .addComponent(OneOffNameLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(OneOffNameInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(User1OneOffEstLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(User1OneOffEstInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(User2OneOffEstLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(User2OneOffEstInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 52, Short.MAX_VALUE)
                .addGroup(AddTaskPageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ConfirmButton)
                    .addComponent(CancelButton))
                .addContainerGap())
        );

        jLayeredPane2.setLayer(HomePage, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane2.setLayer(AddTaskPage, javax.swing.JLayeredPane.DEFAULT_LAYER);

        javax.swing.GroupLayout jLayeredPane2Layout = new javax.swing.GroupLayout(jLayeredPane2);
        jLayeredPane2.setLayout(jLayeredPane2Layout);
        jLayeredPane2Layout.setHorizontalGroup(
            jLayeredPane2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(HomePage, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jLayeredPane2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(AddTaskPage, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jLayeredPane2Layout.setVerticalGroup(
            jLayeredPane2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(HomePage, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jLayeredPane2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(AddTaskPage, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLayeredPane2)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLayeredPane2)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    
    private void AddTaskButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AddTaskButtonActionPerformed

       System.out.println(AllAvailableCommonTasks);
       System.out.println(AllTasks);
       System.out.println(AllSelectedTasks.size());
        System.out.println(SelectedTasks);
        System.out.println(AllAvailableCommonTasks.size());
       
       HomePage.setVisible(false);
       AddTaskPage.setVisible(true);
    }//GEN-LAST:event_AddTaskButtonActionPerformed

    private void RemoveTaskButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RemoveTaskButtonActionPerformed
        RemoveTaskDialog.setVisible(true);
    }//GEN-LAST:event_RemoveTaskButtonActionPerformed

    private void AssignTaskButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AssignTaskButtonActionPerformed
       AssignTasksAlgo();
    }//GEN-LAST:event_AssignTaskButtonActionPerformed

    private void OneOffNameInputActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_OneOffNameInputActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_OneOffNameInputActionPerformed

    private void ConfirmButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ConfirmButtonActionPerformed
        if(!User1CommonTaskEstInput.getText().equals(PlaceholderText)){
            //makes sure user time estimates are entered as integers 
            try{
                Integer.parseInt(User1CommonTaskEstInput.getText());
                Integer.parseInt(User2CommonTaskEstInput.getText());
            }
            catch(NumberFormatException nfe){
                SubmissionFailedDialog.setVisible(true);
                User1CommonTaskEstInput.setText(PlaceholderText);
                User1CommonTaskEstInput.setForeground(new Color(153,153,153));
                User2CommonTaskEstInput.setText(PlaceholderText);
                User2CommonTaskEstInput.setForeground(new Color(153,153,153));
                OneOffNameInput.setText(PlaceholderText);
                OneOffNameInput.setForeground(new Color(153,153,153));
                User1OneOffEstInput.setText(PlaceholderText);
                User1OneOffEstInput.setForeground(new Color(153,153,153));
                User2OneOffEstInput.setText(PlaceholderText);
                User2OneOffEstInput.setForeground(new Color(153,153,153));
            }
        }
        //makes sure that the user time estimates are inputted as integers
        if(!OneOffNameInput.getText().equals(PlaceholderText)){
            
            try{
                Integer.parseInt(User1OneOffEstInput.getText());
                Integer.parseInt(User2OneOffEstInput.getText());
            }
            catch(NumberFormatException nfe){
                SubmissionFailedDialog.setVisible(true);
                User1CommonTaskEstInput.setText(PlaceholderText);
                User1CommonTaskEstInput.setForeground(new Color(153,153,153));
                User2CommonTaskEstInput.setText(PlaceholderText);
                User2CommonTaskEstInput.setForeground(new Color(153,153,153));
                OneOffNameInput.setText(PlaceholderText);
                OneOffNameInput.setForeground(new Color(153,153,153));
                User1OneOffEstInput.setText(PlaceholderText);
                User1OneOffEstInput.setForeground(new Color(153,153,153));
                User2OneOffEstInput.setText(PlaceholderText);
                User2OneOffEstInput.setForeground(new Color(153,153,153)); 
            }
        }
        //Makes sure user has enter either just a common task/one off task or both
        if(!User1CommonTaskEstInput.getText().equals(PlaceholderText) &&
               !User2CommonTaskEstInput.getText().equals(PlaceholderText) &&
                OneOffNameInput.getText().equals(PlaceholderText) &&
                User1OneOffEstInput.getText().equals(PlaceholderText) &&
                User2OneOffEstInput.getText().equals(PlaceholderText)||
               !OneOffNameInput.getText().equals(PlaceholderText) &&
               !User1OneOffEstInput.getText().equals(PlaceholderText) &&
               !User2OneOffEstInput.getText().equals(PlaceholderText) &&
               User1CommonTaskEstInput.getText().equals(PlaceholderText)&&
               User2CommonTaskEstInput.getText().equals(PlaceholderText)||
               !User1CommonTaskEstInput.getText().equals(PlaceholderText) &&
               !User2CommonTaskEstInput.getText().equals(PlaceholderText) &&
               !OneOffNameInput.getText().equals(PlaceholderText) &&
               !User1OneOffEstInput.getText().equals(PlaceholderText) &&
               !User2OneOffEstInput.getText().equals(PlaceholderText)){
                //checks if user has selected a new common task if so will enter the data into the database    
                if(!User1CommonTaskEstInput.getText().equals(PlaceholderText)){
                    CommonTaskName = CommonTaskDropDown.getSelectedItem().toString();
                    CommonTaskUser1Est = Integer.parseInt(User1CommonTaskEstInput.getText());
                    CommonTaskUser2Est = Integer.parseInt(User2CommonTaskEstInput.getText());
                    
                    // runs the sql query to insert the selected common task time estimates into the common task table into the database
                    String sqlString = new String("UPDATE commonTasks "
                                                 + "SET user1_estTime = "+CommonTaskUser1Est+", "
                                                 + "user2_estTime = "+CommonTaskUser2Est+", "
                                                 + "selected = true "
                                                 + "WHERE task_name = '"+CommonTaskName+"';");
                                                       
                    boolean success = database.RunSQL(sqlString);
                
                    //if sql query failed it will output the failed to run message
                    if(!success){
                        System.out.println("Failed to run ");
                    }
                }
                //checks if a user has entered a new one off task if so will enter the data into the database
                if(!OneOffNameInput.getText().equals(PlaceholderText)){
                    OneOffTaskName = OneOffNameInput.getText();
                    OneOffUser1Est = Integer.parseInt(User1OneOffEstInput.getText());
                    OneOffUser2Est = Integer.parseInt(User2OneOffEstInput.getText());
                    
                    // runs the sql query to insert the new one off task into the one off task table into the database
                    String sqlString = new String("INSERT INTO oneOffTasks(task_name, user1_estTime, user2_estTime)"
                            + " VALUES ('"+OneOffTaskName+ "',"+OneOffUser1Est+","+OneOffUser2Est+");");
                    boolean success = database.RunSQL(sqlString);
                
                    //if sql query failed it will output the failed to run message
                    if(!success){
                        System.out.println("Failed to run ");
                    }
                }
                    //resets the input fields back to default when user succesfully submits
                    User1CommonTaskEstInput.setText(PlaceholderText);
                    User1CommonTaskEstInput.setForeground(new Color(153,153,153));
                    User2CommonTaskEstInput.setText(PlaceholderText);
                    User2CommonTaskEstInput.setForeground(new Color(153,153,153));
                    OneOffNameInput.setText(PlaceholderText);
                    OneOffNameInput.setForeground(new Color(153,153,153));
                    User1OneOffEstInput.setText(PlaceholderText);
                    User1OneOffEstInput.setForeground(new Color(153,153,153));
                    User2OneOffEstInput.setText(PlaceholderText);
                    User2OneOffEstInput.setForeground(new Color(153,153,153));
                    
                    
                    SubmitConfirmationDialog.setVisible(true);
                    HomePage.setVisible(true);
                    AddTaskPage.setVisible(false);                
       }
        //resets input fields back to default and shows the submisssion failed dialoge if user unsuccesfully submits
       else{
           SubmissionFailedDialog.setVisible(true);
           User1CommonTaskEstInput.setText(PlaceholderText);
           User1CommonTaskEstInput.setForeground(new Color(153,153,153));
           User2CommonTaskEstInput.setText(PlaceholderText);
           User2CommonTaskEstInput.setForeground(new Color(153,153,153));
           OneOffNameInput.setText(PlaceholderText);
           OneOffNameInput.setForeground(new Color(153,153,153));
           User1OneOffEstInput.setText(PlaceholderText);
           User1OneOffEstInput.setForeground(new Color(153,153,153));
           User2OneOffEstInput.setText(PlaceholderText);
           User2OneOffEstInput.setForeground(new Color(153,153,153));           
       }
    }//GEN-LAST:event_ConfirmButtonActionPerformed

    private void CancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CancelButtonActionPerformed
        //takes user back to the main page and resets the input fields to their default setting
       HomePage.setVisible(true);
       AddTaskPage.setVisible(false);
       User1CommonTaskEstInput.setText(PlaceholderText);
       User1CommonTaskEstInput.setForeground(new Color(153,153,153));
       User2CommonTaskEstInput.setText(PlaceholderText);
       User2CommonTaskEstInput.setForeground(new Color(153,153,153));
       OneOffNameInput.setText(PlaceholderText);
       OneOffNameInput.setForeground(new Color(153,153,153));
       User1OneOffEstInput.setText(PlaceholderText);
       User1OneOffEstInput.setForeground(new Color(153,153,153));
       User2OneOffEstInput.setText(PlaceholderText);
       User2OneOffEstInput.setForeground(new Color(153,153,153)); 

    }//GEN-LAST:event_CancelButtonActionPerformed

    private void NewCommonTaskNameInputActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_NewCommonTaskNameInputActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_NewCommonTaskNameInputActionPerformed

    private void NewCommonTaskNameInputFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_NewCommonTaskNameInputFocusGained
        //changes the input field to blank so user does not have to manually delete the placeholder text
        if(NewCommonTaskNameInput.getText().equals(PlaceholderText)){
            NewCommonTaskNameInput.setText("");
            NewCommonTaskNameInput.setForeground(new Color(0,0,0));
        }
    }//GEN-LAST:event_NewCommonTaskNameInputFocusGained

    private void NewCommonTaskNameInputFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_NewCommonTaskNameInputFocusLost
        //when user clicks off the inout field if no text has been entered it will display the placeholder text again
        if(NewCommonTaskNameInput.getText().equals("")){
            NewCommonTaskNameInput.setText(PlaceholderText);
            NewCommonTaskNameInput.setForeground(new Color(153,153,153));
        }
    }//GEN-LAST:event_NewCommonTaskNameInputFocusLost

    private void NewCommonTaskUser1EstInputFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_NewCommonTaskUser1EstInputFocusGained
        //changes the input field to blank so user does not have to manually delete the placeholder text
        if(NewCommonTaskUser1EstInput.getText().equals(PlaceholderText)){
            NewCommonTaskUser1EstInput.setText("");
            NewCommonTaskUser1EstInput.setForeground(new Color(0,0,0));
        }
    }//GEN-LAST:event_NewCommonTaskUser1EstInputFocusGained

    private void NewCommonTaskUser1EstInputFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_NewCommonTaskUser1EstInputFocusLost
        //when user clicks off the inout field if no text has been entered it will display the placeholder text again
        if(NewCommonTaskUser1EstInput.getText().equals("")){
            NewCommonTaskUser1EstInput.setText(PlaceholderText);
            NewCommonTaskUser1EstInput.setForeground(new Color(153,153,153));
        }
    }//GEN-LAST:event_NewCommonTaskUser1EstInputFocusLost

    private void NewCommonTaskUser1EstInputActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_NewCommonTaskUser1EstInputActionPerformed
  
    }//GEN-LAST:event_NewCommonTaskUser1EstInputActionPerformed

    private void NewCommonTaskUser2EstInputFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_NewCommonTaskUser2EstInputFocusGained
        //changes the input field to blank so user does not have to manually delete the placeholder text
        if(NewCommonTaskUser2EstInput.getText().equals(PlaceholderText)){
            NewCommonTaskUser2EstInput.setText("");
            NewCommonTaskUser2EstInput.setForeground(new Color(0,0,0));
        }        
    }//GEN-LAST:event_NewCommonTaskUser2EstInputFocusGained

    private void NewCommonTaskUser2EstInputFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_NewCommonTaskUser2EstInputFocusLost
        //when user clicks off the inout field if no text has been entered it will display the placeholder text again
        if(NewCommonTaskUser2EstInput.getText().equals("")){
            NewCommonTaskUser2EstInput.setText(PlaceholderText);
            NewCommonTaskUser2EstInput.setForeground(new Color(153,153,153));
        }     
    }//GEN-LAST:event_NewCommonTaskUser2EstInputFocusLost

    private void NewCommonTaskUser2EstInputActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_NewCommonTaskUser2EstInputActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_NewCommonTaskUser2EstInputActionPerformed

    private void AddNewCommonTaskButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AddNewCommonTaskButtonActionPerformed
        //when user clicks the button to add a new common task the dialog will pop up
        AddNewCommonTask.setVisible(true);
    }//GEN-LAST:event_AddNewCommonTaskButtonActionPerformed

    private void NewCommonSubmitButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_NewCommonSubmitButtonActionPerformed
        //makes sure users time estimates are numbers only, if they are not it will show the submission failed 
        try{
            Integer.parseInt(NewCommonTaskUser1EstInput.getText());
            Integer.parseInt(NewCommonTaskUser2EstInput.getText());
        }
        catch(NumberFormatException nfe){
            SubmissionFailedDialog.setVisible(true);
        }
        
        //checks to make sure that the user has inputted all information
        if(!NewCommonTaskNameInput.getText().equals(PlaceholderText)&&
           !NewCommonTaskUser1EstInput.getText().equals(PlaceholderText)&&
           !NewCommonTaskUser2EstInput.getText().equals(PlaceholderText)){
            
                //gets the task name and user estimates, converting the estimates to integers so they can be stored in the database
                NewCommonTaskName = NewCommonTaskNameInput.getText();
                NewCommonTaskUser1Est = Integer.parseInt(NewCommonTaskUser1EstInput.getText());
                NewCommonTaskUser2Est = Integer.parseInt(NewCommonTaskUser2EstInput.getText());
                
                // runs the sql query to insert the new common task into the common task table into the database
                String sqlString = new String("INSERT INTO commonTasks(task_name, user1_estTime,user2_estTime,selected)"
                        + "VALUES ('"+NewCommonTaskName +"',"+NewCommonTaskUser1Est+","+NewCommonTaskUser2Est+", true);");                                                                                     
                     
                boolean success = database.RunSQL(sqlString);
                
                //if sql query failed it will output the failed to run message
                if(!success){
                    System.out.println("Failed to run ");
                }
                
                //upon successful confirmation updates the current arraylists to get the most up to date data 
                ConnectToDatabase();
                GetAllTasks();
                RefreshAllAvailableCommonTasks();
                GetAllSelectedTasks();
                AddNewCommonTask.setVisible(false);
                SubmitConfirmationDialog.setVisible(true);
        }
        else{
            SubmissionFailedDialog.setVisible(true);
        }
    }//GEN-LAST:event_NewCommonSubmitButtonActionPerformed

    private void SubmissionConfirmationOkayButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SubmissionConfirmationOkayButtonActionPerformed
        SubmitConfirmationDialog.setVisible(false);
    }//GEN-LAST:event_SubmissionConfirmationOkayButtonActionPerformed

    private void SubmissionFailedOkayButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SubmissionFailedOkayButtonActionPerformed
        SubmissionFailedDialog.setVisible(false);
    }//GEN-LAST:event_SubmissionFailedOkayButtonActionPerformed

    private void User1CommonTaskEstInputFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_User1CommonTaskEstInputFocusGained
        if(User1CommonTaskEstInput.getText().equals(PlaceholderText)){
            User1CommonTaskEstInput.setText("");
            User1CommonTaskEstInput.setForeground(new Color(0,0,0));
        }     
    }//GEN-LAST:event_User1CommonTaskEstInputFocusGained

    private void User1CommonTaskEstInputFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_User1CommonTaskEstInputFocusLost
        if(User1CommonTaskEstInput.getText().equals("")){
            User1CommonTaskEstInput.setText(PlaceholderText);
            User1CommonTaskEstInput.setForeground(new Color(153,153,153));
        } 
    }//GEN-LAST:event_User1CommonTaskEstInputFocusLost

    private void User2CommonTaskEstInputFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_User2CommonTaskEstInputFocusGained
        if(User2CommonTaskEstInput.getText().equals(PlaceholderText)){
            User2CommonTaskEstInput.setText("");
            User2CommonTaskEstInput.setForeground(new Color(0,0,0));
        } 
    }//GEN-LAST:event_User2CommonTaskEstInputFocusGained

    private void User2CommonTaskEstInputFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_User2CommonTaskEstInputFocusLost
        if(User2CommonTaskEstInput.getText().equals("")){
            User2CommonTaskEstInput.setText(PlaceholderText);
            User2CommonTaskEstInput.setForeground(new Color(153,153,153));
        } 
    }//GEN-LAST:event_User2CommonTaskEstInputFocusLost

    private void RemoveTaskConfirmButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RemoveTaskConfirmButtonActionPerformed
        RemoveSelectedTask = RemoveTaskList.getSelectedItem().toString();
        String sqlString = new String ("UPDATE commonTasks"
                + " SET selected = false"
                + " WHERE task_name = ('"+RemoveSelectedTask+"');");
        
        boolean success = database.RunSQL(sqlString);
                
        //if sql query failed it will output the failed to run message
        if(!success){
            System.out.println("Failed to run ");
        }
                
        String sqlString1 = new String("DELETE FROM oneOffTasks "
                + "WHERE task_name = ('"+RemoveSelectedTask+"');");
        RemoveTaskDialog.setVisible(false);
        boolean success1 = database.RunSQL(sqlString1);
                
        //if sql query failed it will output the failed to run message
        if(!success1){
            System.out.println("Failed to run ");
        }
    }//GEN-LAST:event_RemoveTaskConfirmButtonActionPerformed

    private void RemoveTaskCancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RemoveTaskCancelButtonActionPerformed
        RemoveTaskDialog.setVisible(false);
    }//GEN-LAST:event_RemoveTaskCancelButtonActionPerformed

    private void OneOffNameInputFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_OneOffNameInputFocusGained
        if(OneOffNameInput.getText().equals(PlaceholderText)){
            OneOffNameInput.setText("");
            OneOffNameInput.setForeground(new Color(0,0,0));
        }
    }//GEN-LAST:event_OneOffNameInputFocusGained

    private void OneOffNameInputFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_OneOffNameInputFocusLost
        if(OneOffNameInput.getText().equals("")){
            OneOffNameInput.setText(PlaceholderText);
            OneOffNameInput.setForeground(new Color(153,153,153));
        }
    }//GEN-LAST:event_OneOffNameInputFocusLost

    private void User1OneOffEstInputFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_User1OneOffEstInputFocusGained
        if(User1OneOffEstInput.getText().equals(PlaceholderText)){
            User1OneOffEstInput.setText("");
            User1OneOffEstInput.setForeground(new Color(0,0,0));
        }
    }//GEN-LAST:event_User1OneOffEstInputFocusGained

    private void User1OneOffEstInputFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_User1OneOffEstInputFocusLost
        if(User1OneOffEstInput.getText().equals("")){
            User1OneOffEstInput.setText(PlaceholderText);
            User1OneOffEstInput.setForeground(new Color(153,153,153));
        }
    }//GEN-LAST:event_User1OneOffEstInputFocusLost

    private void User2OneOffEstInputFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_User2OneOffEstInputFocusGained
        if(User2OneOffEstInput.getText().equals(PlaceholderText)){
            User2OneOffEstInput.setText("");
            User2OneOffEstInput.setForeground(new Color(0,0,0));
        }
    }//GEN-LAST:event_User2OneOffEstInputFocusGained

    private void User2OneOffEstInputFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_User2OneOffEstInputFocusLost
        if(User2OneOffEstInput.getText().equals("")){
            User2OneOffEstInput.setText(PlaceholderText);
            User2OneOffEstInput.setForeground(new Color(153,153,153));
        }
    }//GEN-LAST:event_User2OneOffEstInputFocusLost

    private void User1OneOffEstInputActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_User1OneOffEstInputActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_User1OneOffEstInputActionPerformed

    private void RemoveTaskListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RemoveTaskListActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_RemoveTaskListActionPerformed

    private void NewCommonCancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_NewCommonCancelButtonActionPerformed
        AddNewCommonTask.setVisible(false);
    }//GEN-LAST:event_NewCommonCancelButtonActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(UserGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(UserGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(UserGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(UserGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new UserGUI().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JDialog AddNewCommonTask;
    private javax.swing.JButton AddNewCommonTaskButton;
    private javax.swing.JButton AddTaskButton;
    private javax.swing.JPanel AddTaskPage;
    private javax.swing.JButton AssignTaskButton;
    private javax.swing.JButton CancelButton;
    private javax.swing.JComboBox<String> CommonTaskDropDown;
    private javax.swing.JLabel CommonTaskSubtitle;
    private javax.swing.JButton ConfirmButton;
    private javax.swing.JPanel HomePage;
    private javax.swing.JButton NewCommonCancelButton;
    private javax.swing.JButton NewCommonSubmitButton;
    private javax.swing.JTextField NewCommonTaskNameInput;
    private javax.swing.JLabel NewCommonTaskNameLabel;
    private javax.swing.JTextField NewCommonTaskUser1EstInput;
    private javax.swing.JLabel NewCommonTaskUser1EstLabel;
    private javax.swing.JTextField NewCommonTaskUser2EstInput;
    private javax.swing.JLabel NewCommonTaskUser2EstLabel;
    private javax.swing.JTextField OneOffNameInput;
    private javax.swing.JLabel OneOffNameLabel;
    private javax.swing.JLabel OneOffTaskSubtitle;
    private javax.swing.JButton RemoveTaskButton;
    private javax.swing.JButton RemoveTaskCancelButton;
    private javax.swing.JButton RemoveTaskConfirmButton;
    private javax.swing.JDialog RemoveTaskDialog;
    private javax.swing.JLabel RemoveTaskLabel;
    private javax.swing.JComboBox<String> RemoveTaskList;
    private javax.swing.JLabel SubmissionConfirmationLabel;
    private javax.swing.JButton SubmissionConfirmationOkayButton;
    private javax.swing.JDialog SubmissionFailedDialog;
    private javax.swing.JLabel SubmissionFailedLabel;
    private javax.swing.JButton SubmissionFailedOkayButton;
    private javax.swing.JDialog SubmitConfirmationDialog;
    private javax.swing.JTable TaskTable;
    private javax.swing.JLabel Title;
    private javax.swing.JTextField User1CommonTaskEstInput;
    private javax.swing.JLabel User1CommonTaskEstLabel;
    private javax.swing.JTextField User1OneOffEstInput;
    private javax.swing.JLabel User1OneOffEstLabel;
    private javax.swing.JTextField User2CommonTaskEstInput;
    private javax.swing.JLabel User2CommonTaskEstLabel;
    private javax.swing.JTextField User2OneOffEstInput;
    private javax.swing.JLabel User2OneOffEstLabel;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLayeredPane jLayeredPane2;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables


}
