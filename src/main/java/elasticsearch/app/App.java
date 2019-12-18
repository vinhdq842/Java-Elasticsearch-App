package elasticsearch.app;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JButton;

/**
 * @author ServantOfEvil
 */

public class App implements ActionListener {

    private JFrame mainFrame;
    private JFrame add_edit_Frame, search_Frame;
    private Vector<Category> categories;
    private JButton btnNew, btnEdit, btnDelete, btnSearch, btnExit;
    private JTextField textFieldPostDate;
    private JTextField textFieldCode;
    private JTextField textFieldAuthor;
    private JTextField textFieldTitle;
    private JTextArea textAreaContent;
    private JButton btnOkAddEdit;
    private JButton btnSearchButton;
    private static TransportClient client;
    private int i = -1;

    public static void main(String[] args) {
        Settings settings = Settings.builder().put("cluster.name", "docker-cluster").build();
        try {
            client = new PreBuiltTransportClient(settings).addTransportAddress(new TransportAddress(InetAddress.getByName("192.168.1.117"), 9300));
            //192.168.1.117
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        new App();
    }

    public static void insert(Category cat, String id, News news) {
        try {
            client.prepareIndex(cat.getCode(), "_doc", id)
                    .setSource(jsonBuilder()
                            .startObject()
                            .field("tieu_de", news.getTitle())
                            .field("ma_tin_bai", news.getNewsCode())
                            .field("noi_dung", news.getContent())
                            .field("ngay_dang", news.getDate())
                            .field("tac_gia", news.getAuthor())
                            .endObject())
                    .get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<News> search(Category cat, String title, String content, String date, String author) {

        SearchResponse result;
        SearchRequestBuilder requestBuilder = client.prepareSearch(cat.getCode()).setSearchType(SearchType.DFS_QUERY_THEN_FETCH);

        if (!"".equals(title))
            requestBuilder.setQuery(QueryBuilders.matchQuery("tieu_de", title));
        if (!"".equals(content))
            requestBuilder.setQuery(QueryBuilders.matchQuery("noi_dung", content));
        if (!"".equals(author))
            requestBuilder.setQuery(QueryBuilders.matchQuery("tac_gia", author));
        if (!"".equals(date))
            requestBuilder.setQuery(QueryBuilders.matchQuery("ngay_dang", date))
                    .setFrom(0)
                    .setSize(60)
                    .setExplain(true);

        result = requestBuilder.get();
        SearchHit[] results = result.getHits().getHits();
        ArrayList<News> rsArrayList = new ArrayList<News>();

        for (SearchHit hit : results) {
            Map<String, Object> rsMap = hit.getSourceAsMap();
            if (rsMap != null)
                rsArrayList.add(new News(hit.getId(), rsMap.get("tieu_de").toString(), rsMap.get("ma_tin_bai").toString(), rsMap.get("noi_dung").toString(), rsMap.get("ngay_dang").toString(), rsMap.get("tac_gia").toString()));
        }

        return rsArrayList;
    }

    public static ArrayList<News> getAllDocs(ArrayList<News> arr, Category cat) {
        arr.clear();
        int scrollSize = 10;
        SearchResponse response = null;
        int i = 0;
        while (response == null || response.getHits().getHits().length != 0) {
            response = client.prepareSearch(cat.getCode()).setQuery(QueryBuilders.matchAllQuery()).setSize(scrollSize).setFrom(i * scrollSize).execute().actionGet();
            for (SearchHit hit : response.getHits()) {
                Map<String, Object> rsMap = hit.getSourceAsMap();
                arr.add(new News(hit.getId(), rsMap.get("tieu_de").toString(), rsMap.get("ma_tin_bai").toString(), rsMap.get("noi_dung").toString(), rsMap.get("ngay_dang").toString(), rsMap.get("tac_gia").toString()));
            }
            i++;
        }
        return arr;
    }

    App() {
        mainFrame = new JFrame();
        mainFrame.setTitle("Elasticsearch App");
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            // TODO: handle exception
        }
        mainFrame.setBounds(new Rectangle(255, 100, 700, 500));
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        categories = new Vector<Category>();
        categories.add(new Category(1, "am-thuc", "Ẩm thực"));
        categories.add(new Category(2, "thoi-trang", "Thời trang"));
        categories.add(new Category(3, "xa-hoi", "Xã hội"));
        categories.add(new Category(4, "giai-tri", "Giải trí"));

        JTabbedPane tabbedPane = new JTabbedPane();

        for (Category cat : categories)
            tabbedPane.add(cat);

        mainFrame.getContentPane().add(new JScrollPane(tabbedPane));

        JMenuBar menuBar = new JMenuBar();
        mainFrame.setJMenuBar(menuBar);

        btnNew = new JButton("New");
        menuBar.add(btnNew);

        btnEdit = new JButton("Edit");
        menuBar.add(btnEdit);

        btnDelete = new JButton("Delete");
        menuBar.add(btnDelete);

        btnSearch = new JButton("Search");
        menuBar.add(btnSearch);

        btnExit = new JButton("Exit");
        menuBar.add(btnExit);

        btnNew.addActionListener(this);
        btnDelete.addActionListener(this);
        btnEdit.addActionListener(this);
        btnSearch.addActionListener(this);
        btnExit.addActionListener(this);

        mainFrame.setVisible(true);

        initComponents();
        initAddFrame();
        initSearchFrame();
    }

    private void initComponents() {
        textFieldTitle = new JTextField();
        textFieldTitle.setBounds(85, 26, 86, 25);
        textFieldTitle.setColumns(10);

        textFieldCode = new JTextField();
        textFieldCode.setBounds(300, 26, 86, 25);
        textFieldCode.setColumns(10);

        textAreaContent = new JTextArea();
        textAreaContent.setBounds(35, 78, 136, 130);
        textAreaContent.setLineWrap(true);

        textFieldPostDate = new JTextField();
        textFieldPostDate.setBounds(300, 51, 86, 25);
        textFieldPostDate.setColumns(10);

        textFieldAuthor = new JTextField();
        textFieldAuthor.setBounds(300, 80, 124, 25);
        textFieldAuthor.setColumns(10);

        btnOkAddEdit = new JButton("OK");
        btnOkAddEdit.setBounds(300, 158, 89, 23);
        btnOkAddEdit.addActionListener(this);

        btnSearchButton = new JButton("Search");
        btnSearchButton.setBounds(300, 158, 89, 23);
        btnSearchButton.addActionListener(this);
    }

    private void initAddFrame() {
        add_edit_Frame = new JFrame();
        add_edit_Frame.setBounds(200, 200, 500, 300);
        add_edit_Frame.getContentPane().setLayout(null);

        JLabel lblTitle = new JLabel("Title:");
        lblTitle.setBounds(45, 29, 46, 14);

        add_edit_Frame.getContentPane().add(lblTitle);
        add_edit_Frame.getContentPane().add(textFieldTitle);

        JLabel lblContent = new JLabel("Content:");
        lblContent.setBounds(25, 54, 46, 14);

        add_edit_Frame.getContentPane().add(lblContent);
        add_edit_Frame.getContentPane().add(textAreaContent);

        JLabel lblCode = new JLabel("Code:");
        lblCode.setBounds(245, 29, 46, 14);

        add_edit_Frame.getContentPane().add(lblCode);
        add_edit_Frame.getContentPane().add(textFieldCode);


        JLabel lblPostDate = new JLabel("Post Date:");
        lblPostDate.setBounds(220, 54, 60, 14);

        add_edit_Frame.getContentPane().add(lblPostDate);
        add_edit_Frame.getContentPane().add(textFieldPostDate);

        JLabel lblAuthor = new JLabel("Author:");
        lblAuthor.setBounds(237, 83, 46, 14);

        add_edit_Frame.getContentPane().add(lblAuthor);
        add_edit_Frame.getContentPane().add(textFieldAuthor);

        add_edit_Frame.getContentPane().add(btnOkAddEdit);

    }

    private void initSearchFrame() {
        search_Frame = new JFrame();
        search_Frame.getContentPane().setLayout(null);
        search_Frame.setBounds(200, 200, 500, 300);
        search_Frame.setTitle("Search");

        JLabel lblTitle = new JLabel("Title:");
        lblTitle.setBounds(45, 29, 46, 14);

        JLabel lblContent = new JLabel("Content:");
        lblContent.setBounds(25, 54, 46, 14);

        JLabel lblPostDate = new JLabel("Post Date:");
        lblPostDate.setBounds(220, 54, 60, 14);

        JLabel lblAuthor = new JLabel("Author:");
        lblAuthor.setBounds(237, 83, 46, 14);

        search_Frame.add(lblAuthor);
        search_Frame.add(textFieldAuthor);

        search_Frame.add(lblPostDate);
        search_Frame.add(textFieldPostDate);

        search_Frame.add(lblContent);
        search_Frame.add(textAreaContent);

        search_Frame.add(lblTitle);
        search_Frame.add(textFieldTitle);

        search_Frame.add(btnSearchButton);
    }

    public void actionPerformed(ActionEvent e) {
        JButton action = (JButton) e.getSource();
        int index = ((JTabbedPane) ((JScrollPane) mainFrame.getContentPane().getComponent(0)).getViewport().getView()).getSelectedIndex();

        if (action == btnNew) {

            initAddFrame();
            i = -1;
            add_edit_Frame.setTitle("New");
            textFieldTitle.setText("");
            textFieldCode.setText("");
            textFieldAuthor.setText("");
            textFieldPostDate.setText("");
            textAreaContent.setText("");
            add_edit_Frame.setVisible(true);

        } else if (action == btnEdit) {

            initAddFrame();
            add_edit_Frame.setTitle("Edit");
            String string = "";
            try {
                string = JOptionPane.showInputDialog("Edit at:");
            } catch (Exception e2) {
                // TODO: handle exception
            }
            if (!"".contentEquals(string)) {
                i = Integer.parseInt(string);
                News n = categories.elementAt(index).getNews().get(i);
                i = Integer.parseInt(n.getId());
                textFieldTitle.setText(n.getTitle());
                textFieldCode.setText(n.getNewsCode());
                textFieldAuthor.setText(n.getAuthor());
                textFieldPostDate.setText(n.getDate());
                textAreaContent.setText(n.getContent());
                add_edit_Frame.setVisible(true);
            }

        } else if (action == btnOkAddEdit) {

            if (i == -1)
                categories.elementAt(index).addNews(textFieldTitle.getText(), textFieldCode.getText(), textAreaContent.getText(), textFieldPostDate.getText(), textFieldAuthor.getText());
            else
                categories.elementAt(index).addNews(i, textFieldTitle.getText(), textFieldCode.getText(), textAreaContent.getText(), textFieldPostDate.getText(), textFieldAuthor.getText());

            add_edit_Frame.setVisible(false);

        } else if (action == btnSearch) {

            initSearchFrame();
            search_Frame.setVisible(true);

        } else if (action == btnSearchButton) {

            ArrayList<News> rs = search(categories.elementAt(index), textFieldTitle.getText(), textAreaContent.getText(), textFieldPostDate.getText(), textFieldAuthor.getText());

            for (News n : rs) {
                System.out.println(n.toString());
                JLabel titJLabel = new JLabel(n.getTitle());
                titJLabel.setFont(new Font("TimesRoman", Font.BOLD, 18));
                JLabel dateJLabel = new JLabel("posted on ".concat(n.getDate()));
                dateJLabel.setFont(new Font("TimesRoman", Font.ITALIC, 10));
                JLabel authorJLabel = new JLabel("by ".concat(n.getAuthor()));
                authorJLabel.setFont(new Font("TimesRoman", Font.ITALIC, 10));
                search_Frame.add(titJLabel);
                search_Frame.add(new JLabel(n.getContent()));
                search_Frame.add(dateJLabel);
                search_Frame.add(authorJLabel);
                search_Frame.add(new JLabel("----------------------------"));
            }

        } else if (action == btnDelete) {

            Category cat = categories.elementAt(index);
            client.prepareDelete(cat.getCode(), "_doc", cat.getNews().get(Integer.parseInt(JOptionPane.showInputDialog("Delete document at:"))).getId()).execute().actionGet();

        } else if (action == btnExit) {

            client.close();
            System.exit(0);

        }


        categories.elementAt(index).loadNews();
        categories.elementAt(index).feedToPane();
        SwingUtilities.updateComponentTreeUI(mainFrame);
    }

}

class Category extends JPanel {

    private static final long serialVersionUID = 1L;
    private int id;
    private String name;
    private String code;
    private ArrayList<News> news;

    public Category(int id, String code, String name) {
        this.id = id;
        this.code = code;
        this.name = name;
        news = new ArrayList<News>();
        new JPanel();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        /*
         * App.insert(this, "1", new
         * News("Xe tải bất ngờ lật nghiêng, cá  vương vãi khắp nơi trên đường",
         * "xe-tai-bat-ngo-lat-nghieng-ca-vuong-vai-khap-noi-tren-duong",
         * "Thời điểm trên, xe tải BKS 34C – 139.05 do tài xế Vương Khắc Cường (SN 1975, quê tỉnh Hải Dương) đi hướng Hòa Bình – Hà Nội bất ngờ bị lật nằm ngang trên quốclộ.  Lúc này, các túi lớn trên thùng xe bị vỡ ra, cá trong túi tràn ra bênngoài vương vãi khắp nơi trên đường."
         * , "2018-08-08", "Đàm Quang"));
         */

        loadNews();
        feedToPane();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public void addNews(String title, String code, String content, String date, String author) {
        News n = new News(title, code, content, date, author);
        App.insert(this, String.valueOf(news.size() + 1), n);
    }

    public void addNews(int index, String title, String code, String content, String date, String author) {
        News n = new News(title, code, content, date, author);
        App.insert(this, String.valueOf(index), n);
        System.out.println(index);
    }

    public ArrayList<News> getNews() {
        return news;
    }

    void loadNews() {
        App.getAllDocs(news, this);
    }

    void feedToPane() {
        this.removeAll();
        for (News n : news) {
            JLabel titJLabel = new JLabel(n.getTitle());
            titJLabel.setFont(new Font("TimesRoman", Font.BOLD, 18));
            JLabel dateJLabel = new JLabel("posted on ".concat(n.getDate()));
            dateJLabel.setFont(new Font("TimesRoman", Font.ITALIC, 10));
            JLabel authorJLabel = new JLabel("by ".concat(n.getAuthor()));
            authorJLabel.setFont(new Font("TimesRoman", Font.ITALIC, 10));
            this.add(titJLabel);
            this.add(new JLabel(n.getContent()));
            this.add(dateJLabel);
            this.add(authorJLabel);
            this.add(new JLabel("----------------------------"));
        }
    }


}

class News {
    private String id;
    private String title;
    private String newsCode;
    private String content;
    private String date;
    private String author;

    public News(String id, String title, String newsCode, String content, String date, String author) {
        this(title, newsCode, content, date, author);
        this.id = id;
    }

    public News(String title, String newsCode, String content, String date, String author) {
        this.title = title;
        this.author = author;
        this.newsCode = newsCode;
        this.date = date;
        this.content = content;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNewsCode() {
        return newsCode;
    }

    public void setNewsCode(String newsCode) {
        this.newsCode = newsCode;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }


    @Override
    public String toString() {
        return "Title: " + title + ", Code:" + newsCode + ", Content: " + content + ", Post Date: " + date + ", Author: " + author;
    }

}