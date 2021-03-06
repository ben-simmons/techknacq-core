package edu.isi.techknacq.readinglist;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.isi.techknacq.graph.ConceptDepth;
import edu.isi.techknacq.graph.Node;
import edu.isi.techknacq.graph.ReadGraph;
import edu.isi.techknacq.topic.WeightPair;
import edu.isi.techknacq.topic.WordPair;


public class ReadingListJson {
    private Map<String, Double> paperPageRank;
    private ArrayList<ArrayList<WordPair>> wordInTopic;
    private List<String> topickeys;
    private List<Integer> hittopic;
    private Map<String, String> docmap;
    private List []topicToDocs;
    private ArrayList<String> docfiles;
    private int []ordertopic;
    private HashSet<String> authorlists;
    private Logger logger = Logger.getLogger(ReadingListJson.class.getName());

    public void readData(String keyword, String keyname, String pagerankfile,
                         String docfile, int dnum, String doc2conceptfile,
                         String filterfile) {
        KeywordToConcept match1 = new KeywordToConcept();
        match1.readKey(keyname);
        hittopic = match1.getMatch(keyword);
        this.wordInTopic = match1.getWeightTopic();
        this.topickeys = match1.getTopics();
        readPageRankScore(pagerankfile);
        ReadDocumentKey rdk = new ReadDocumentKey(docfile);
        rdk.readFile();
        docmap = rdk.getDocMap();
        ConceptToDoc Getdoc = new ConceptToDoc();
        Getdoc.initNum(topickeys.size());
        Getdoc.addFilter(filterfile);
        Getdoc.getTopK(dnum * 10, doc2conceptfile);
        topicToDocs = Getdoc.getTopic2Doc();
        docfiles = Getdoc.getDocName();
    }

    public String getDocMeta(String id) {
        if (this.docmap.containsKey(id))
            return this.docmap.get(id);
        else
            return null;
    }

    public ArrayList<Integer> getDocs(int tindex) {
        ArrayList<Integer> mydocs = new ArrayList(topicToDocs[tindex].size());
        for (int i = 0; i < topicToDocs[tindex].size(); i++) {
            WeightPair o = (WeightPair)topicToDocs[tindex].get(i);
            mydocs.add(o.getIndex());
        }
        return mydocs;
    }

    public void readPageRankScore(String filename) {
        try {
            this.paperPageRank = new HashMap(this.topickeys.size());
            FileInputStream fstream1;
            fstream1 = new FileInputStream(filename);
            // Get the object of DataInputStream
            DataInputStream in1 = new DataInputStream(fstream1);
            BufferedReader br = new BufferedReader(new InputStreamReader(in1));
            String strline;
            br.readLine(); // Skip node vertices line
            br.readLine(); // Skip column name line
            String keyname;
            double value;
            String sr;
            while ((strline = br.readLine()) != null) {
                Scanner sc = new Scanner(strline);
                sc.useDelimiter("\t| ");
                sr = sc.next();
                if (sr.contains("*Edge") || sr.contains("*Arc"))
                    break;
                keyname = sc.next();
                keyname = keyname.substring(1, keyname.length() - 1);
                value = sc.nextDouble();
                if (!this.paperPageRank.containsKey(keyname)) {
                    this.paperPageRank.put(keyname, value);
                }
            }
            in1.close();
        } catch (FileNotFoundException ex) {
            logger.log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    public String printTopics(int tindex) {
        String topicname;
        topicname = "\"topic\": \n[";
        double minvalue = 1;
        double maxvalue = 0;
        for (int i = 0; i < this.wordInTopic.get(tindex).size(); i++) {
            WordPair w = wordInTopic.get(tindex).get(i);
            double value = w.getprob();
            if (value > maxvalue)
                maxvalue = value;
            if (value < minvalue) {
                minvalue = value;
            }
        }
        for (int i = 0; i < this.wordInTopic.get(tindex).size(); i++) {
            WordPair w = wordInTopic.get(tindex).get(i);
            String word = w.getWord();
            double value = w.getprob();
            topicname += "{";
            topicname += "\"word\": \"" + word + "\",";
            topicname += "\"value\": " + value + "}";
            if (i < this.wordInTopic.get(tindex).size() - 1)
                topicname += ",";
        }
        topicname += "],";
        return topicname;
    }

    public String extractAuthor(String metadata) {
        int index1 = metadata.indexOf("author:");
        int index2 = metadata.indexOf("title:");
        String author;
        if (index1 >= 0 && index2 >= 0) {
            author = metadata.substring(index1 + 8, index2);
        } else
            author = null;
        return author;
    }

    public String printDocName(String metadata, String did, double score) {
        String name;
        int index1 = metadata.indexOf("author:");
        int index2 = metadata.indexOf("title:");
        String author;
        String title;
        if (index1 >= 0 && index2 >= 0) {
            author = metadata.substring(index1 + 8, index2);
        } else
            author = null;
        if (index2 >= 0) {
            title = metadata.substring(index2 + 7, metadata.length());
        } else
            title = null;
        name = "\n\t\t{";
        name += "\"author\": \"" + author + "\", \"title\": \"" + title +
                "\",\"ID\": \"" + did + "\"},";
        return name;
    }

    public String getTopDoc(int tindex, int dnum, List mylist,
                            boolean [] isvisit) {
        ArrayList<Integer> mydocs = this.getDocs(tindex);
        mylist.clear();
        String docstring = "";
        for (Integer mydoc : mydocs) {
            int Did = mydoc;
            if (isvisit[Did])
                continue;
            String dockey = this.docfiles.get(Did);
            double value;
            if (this.paperPageRank.containsKey(dockey))
                value = this.paperPageRank.get(dockey);
            else
                value = -1;
            if (value > -1)
                mylist.add(new WeightPair(value,Did));
        }
        docstring += "\n\"documents\": [";
        int j = 0;
        int dcount = 0;
        Collections.sort(mylist);
        while (dcount < dnum && j < mylist.size() && dcount < mylist.size()) {
            WeightPair o = (WeightPair)mylist.get(j);
            int Did = o.getIndex();
            isvisit[Did] = true;
            String dfile = docfiles.get(Did);
            String metavalue = this.getDocMeta(dfile);
            String author = this.extractAuthor(metavalue);
            if (!this.authorlists.contains(author)) {
                String name = this.printDocName(metavalue, dfile,
                                                o.getWeight());
                docstring += name;
                this.authorlists.add(author);
                dcount++;
            }
            j++;
        }
        docstring = docstring.substring(0, docstring.length() - 1);
        docstring += "],";
        return docstring;
    }

    public void run(String keyword, String graphfile, int maxtopic, int dnum) {
        try {
            FileWriter fstream = new FileWriter(keyword + "_readinglist.json",
                                                false);
            BufferedWriter out = new BufferedWriter(fstream);
            ReadGraph myreader = new ReadGraph(graphfile);
            Node []G = myreader.getGraph();
            this.ordertopic = myreader.orderNode();
            ConceptDepth Dependency = new ConceptDepth();
            Dependency.initGraph(G);
            Dependency.initTopics(this.topickeys);
            boolean []isvisit = new boolean[this.docfiles.size()];
            for (int i = 0; i < isvisit.length; i++) {
                isvisit[i] = false;
            }
            /*
             * Get matched topic and dependent topics
             */
            char []istopicvisit = new char[this.topickeys.size()];
            Arrays.fill(istopicvisit, 'v');
            List mylist = new ArrayList(100);
            this.authorlists = new HashSet();
            out.write("{");
            out.write("\"keyword\": \"" + keyword + "\",\n");
            for (int i = 0; i < hittopic.size(); i++) {
                out.write("\"Match topics\": {\n\t");
                int tindex = hittopic.get(i);
                istopicvisit[tindex] = 'm';
                out.write(this.printTopics(tindex));
                out.write(this.getTopDoc(tindex, dnum, mylist, isvisit));
                ArrayList<Integer> deptopics =
                    Dependency.getTopNode(maxtopic, tindex);
                out.write("\n\t\t\"Dependency topics\": \n[");
                for (int j = 0; j < deptopics.size(); j++) {
                    out.write("{");
                    int ddtindex = deptopics.get(j);
                    if (istopicvisit[ddtindex] != 'm')
                        istopicvisit[ddtindex] = 'd';
                    out.write(this.printTopics(ddtindex));
                    String docstrs = this.getTopDoc(ddtindex, dnum, mylist,
                                                    isvisit);
                    docstrs = docstrs.substring(0, docstrs.length() - 1);
                    if (j < deptopics.size() - 1) {
                        out.write(docstrs);
                        out.write("},\n");
                    } else {
                        out.write(docstrs);
                        out.write("}\n");
                    }
                }
                out.write("]\n");
                if (i < hittopic.size() - 1)
                    out.write("},\n");
                else
                    out.write("}\n");
            }
            String s = Dependency.getSubgraphInString(keyword);
            System.out.println(s);
            out.write("}");
            out.close();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String []args) {
        if (args.length < 6) {
            System.out.println("Usage [keyword] [doc2topic] [topickey] " +
                               "[topicgraph] [dockey] [pagerankfile] " +
                               "[docs/topic] [max_topic] [filterfile]");
            System.exit(2);
        }
        int dnum = 3;
        int maxtnum = 10;
        String filterfile = "yes-no.csv";
        if (args.length > 6)
            dnum = Integer.parseInt(args[6]);
        if (args.length > 7)
            maxtnum = Integer.parseInt(args[7]);
        if (args.length > 8)
            filterfile = args[8];
        ReadingListJson myreadinglist = new ReadingListJson();
        // String keyword, String keyname, String pagerankfile,
        // String docfile, int dnum, String doc2conceptfile
        myreadinglist.readData(args[0], args[2], args[5], args[4], dnum,
                               args[1], filterfile);
        // String keyword, String graphfile, int maxtopic, int dnum
        myreadinglist.run(args[0], args[3], maxtnum, dnum);
    }
}
