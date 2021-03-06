package pmb.music.AllMusic.view.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pmb.music.AllMusic.exception.MajorException;
import pmb.music.AllMusic.model.RecordType;
import pmb.music.AllMusic.utils.BatchUtils;
import pmb.music.AllMusic.utils.Constant;
import pmb.music.AllMusic.utils.FilesUtils;
import pmb.music.AllMusic.utils.MiscUtils;
import pmb.music.AllMusic.view.ComponentBuilder;
import pmb.music.AllMusic.view.PanelUtils;

/**
 * Onglet pour lancer des traitements.
 * <ul>
 * Batchs List :
 * <li>Find duplicate compositions: FDC</li>
 * <li>Mass deletion: for deleting an important amount of compositions</li>
 * <li>Find duplicate files: FDF</li>
 * <li>Missing XML files: MXF</li>
 * <li>Top year: top</li>
 * <li>Finds suspicious compositions: suspicious</li>
 * <li>Finds and edits composition with a slash in title and alone:
 * slashEdit</li>
 * <li>Finds duplicates title with different artist: duplicateTitle</li>
 * <li>Checks filenames (Author + name + publishYear): validateFileName</li>
 * <li>Calculates in a file for each compositions, the number of files
 * associates to it and get the average for the file: averageOfFilesByFiles</li>
 * <li>Looks for weird file size: weirdFileSize</li>
 * <li>Générer des statistiques: stats</li>
 * <li>Checks if deleted</li>
 * <li>By looking in other files, guess the type of unknown compositions:
 * findUnknown
 * </ul>
 *
 * @author PBR
 */
public class BatchPanel extends JPanel {
    private static final long serialVersionUID = -7659089306956006760L;
    private static final Logger LOG = LogManager.getLogger(BatchPanel.class);

    /**
     * Les messages.
     */
    private JTextArea resultLabel;
    private List<String> resultLabelData;
    private String fileResult = Constant.BATCH_FILE;
    private JPanel root;

    /**
     * Open the result file in notepad.
     */
    private JButton batchFileBtn;

    /**
     * Constructor of {@link BatchPanel}.
     */
    public BatchPanel() {
        super();
        LOG.debug("Start BatchPanel");
        this.setLayout(new BorderLayout());
        root = new JPanel();
        this.add(new JScrollPane(root), BorderLayout.CENTER);
        root.setLayout(new GridLayout(15, 1));

        lastLine();
        findDuplicateComposition();
        massDeletion();
        findDuplicateFiles();
        missingXmlFiles();
        topYear();
        suspicious();
        slashEdit();
        duplicateTitle();
        validateFileName();
        averageOfFilesByFiles();
        weirdFileSize();
        stats();
        checksIfDeleted();
        findUnknown();

        LOG.debug("End BatchPanel");
    }

    /**
     * Initialise les composants pour trouver les compositions en double (FDC).
     */
    private void findDuplicateComposition() {
        JPanel fdc = PanelUtils.createBoxLayoutPanel(BoxLayout.X_AXIS);

        JLabel fdcLabel = new JLabel("Recherche les compositions en double: ");
        PanelUtils.addComponent(fdc, fdcLabel, Component.LEFT_ALIGNMENT, 100);

        // Checkbox song
        JLabel fdcSongLabel = new JLabel("Chanson: ");
        JCheckBox fdcSong = new JCheckBox();
        fdcSong.setSelected(true);
        PanelUtils.addComponent(fdc, fdcSongLabel, Component.LEFT_ALIGNMENT, 0);
        PanelUtils.addComponent(fdc, fdcSong, Component.LEFT_ALIGNMENT, 100);

        // Checkbox album
        JLabel fdcAlbumLabel = new JLabel("Album: ");
        JCheckBox fdcAlbum = new JCheckBox();
        fdcAlbum.setSelected(true);
        PanelUtils.addComponent(fdc, fdcAlbumLabel, Component.LEFT_ALIGNMENT, 0);
        PanelUtils.addComponent(fdc, fdcAlbum, Component.LEFT_ALIGNMENT, 100);

        // Checkbox unmergeable
        JLabel fdcUnmergeableLabel = new JLabel("Ignorer les fichier non mergeables: ");
        JCheckBox fdcUnmergeable = new JCheckBox();
        fdcUnmergeable.setSelected(true);
        PanelUtils.addComponent(fdc, fdcUnmergeableLabel, Component.LEFT_ALIGNMENT, 0);
        PanelUtils.addComponent(fdc, fdcUnmergeable, Component.LEFT_ALIGNMENT, 100);

        // Checkbox year
        JLabel fdcYearLabel = new JLabel("Uniquement les fichiers YEAR: ");
        JCheckBox fdcYear = new JCheckBox();
        PanelUtils.addComponent(fdc, fdcYearLabel, Component.LEFT_ALIGNMENT, 0);
        PanelUtils.addComponent(fdc, fdcYear, Component.LEFT_ALIGNMENT, 100);

        // Launch Button
        JButton fdcBtn = ComponentBuilder.buildJButton("Go Compositions En Double", 200, Constant.ICON_GO);
        fdcBtn.setToolTipText("Fusionne les compositions identiques mais non détectées à la fusion classique.");
        fdcBtn.addActionListener((ActionEvent arg0) -> {
            LOG.debug("Start findDuplicateComposition");
            displayText("Start findDuplicateComposition: " + MiscUtils.getCurrentTime(), false);
            new Thread(() -> {
                OngletPanel.getArtist().interruptUpdateArtist(true);
                fileResult = BatchUtils.detectsDuplicateFinal(fdcSong.isSelected(), fdcAlbum.isSelected(),
                        fdcUnmergeable.isSelected(), fdcYear.isSelected(), this);
                OngletPanel.getFichier().updateData();
                OngletPanel.getArtist().updateArtistPanel();
                displayText("End findDuplicateComposition: " + MiscUtils.getCurrentTime(), false);
                LOG.debug("End findDuplicateComposition");
            }).start();
        });
        PanelUtils.addComponent(fdc, fdcBtn, Component.RIGHT_ALIGNMENT, 100);

        root.add(fdc);
    }

    /**
     * Mass deletion: import a file of compositions to delete, by type. A search and
     * a confirmation is done for each.
     */
    private void massDeletion() {
        JPanel massDeletion = PanelUtils.createBoxLayoutPanel(BoxLayout.X_AXIS);

        // Label
        JLabel massDeletionLabel = new JLabel("Suppression des compositions en masse: ");
        PanelUtils.addComponent(massDeletion, massDeletionLabel, Component.LEFT_ALIGNMENT, 100);

        // File chooser
        JLabel selectedFile = new JLabel();
        buildFileChooser(massDeletion, "Charge un fichier csv contenant des compositions", selectedFile,
                new FileNameExtensionFilter("csv", "csv"));

        // Type
        JComboBox<RecordType> type = buildTypeComboBox(massDeletion);

        // Bouton d'action
        JButton massDeletionBtn = ComponentBuilder.buildJButton("Go Mass Deletion", 200, Constant.ICON_GO);
        massDeletionBtn.setToolTipText("Supprime en masse des compositions.");
        massDeletionBtn.addActionListener((ActionEvent arg0) -> {
            if (selectedFile.getText() != null
                    && !StringUtils.equalsIgnoreCase(selectedFile.getText(), Constant.getResourcesDir())) {
                LOG.debug("End browse");
                displayText("Start massDeletion: " + MiscUtils.getCurrentTime(), false);
                new Thread(() -> {
                    fileResult = BatchUtils.massDeletion(type.getSelectedItem().toString(),
                            new File(selectedFile.getText()));
                    displayText("End massDeletion: " + MiscUtils.getCurrentTime(), false);
                }).start();
            } else {
                displayText("No selected file", false);
            }
        });
        PanelUtils.addComponent(massDeletion, massDeletionBtn, Component.RIGHT_ALIGNMENT, 100);

        root.add(massDeletion);
    }

    private static JComboBox<RecordType> buildTypeComboBox(JPanel parent) {
        JPanel typePanel = PanelUtils.createBoxLayoutPanel(BoxLayout.Y_AXIS);
        JLabel typeLabel = ComponentBuilder.buildJLabel("Type : ", 50).orElse(null);
        JComboBox<RecordType> type = new JComboBox<>(new RecordType[] { RecordType.SONG, RecordType.ALBUM });
        PanelUtils.setSize(type, 100, ComponentBuilder.COMPONENT_HEIGHT);
        PanelUtils.addComponent(typePanel, typeLabel, Component.LEFT_ALIGNMENT, 0);
        PanelUtils.addComponent(typePanel, type, Component.LEFT_ALIGNMENT, 0);
        PanelUtils.addComponent(parent, typePanel, Component.RIGHT_ALIGNMENT, 100);
        return type;
    }

    /**
     * Initialise les composants pour trouver les fichiers en double (FDF).
     */
    private void findDuplicateFiles() {
        JPanel fdf = PanelUtils.createBoxLayoutPanel(BoxLayout.X_AXIS);

        // Label
        JLabel fdfLabel = new JLabel("Recherche les fichiers en double: ");
        PanelUtils.addComponent(fdf, fdfLabel, Component.LEFT_ALIGNMENT, 700);

        // Bouton d'action
        JButton fdfBtn = ComponentBuilder.buildJButton("Go Fichiers En Double", 200, Constant.ICON_GO);
        fdfBtn.setToolTipText("Cherche les fichiers en double.");
        fdfBtn.addActionListener((ActionEvent arg0) -> {
            displayText("Start findDuplicateFiles: " + MiscUtils.getCurrentTime(), false);
            new Thread(() -> {
                fileResult = BatchUtils.findDuplicateFiles();
                displayText("End findDuplicateFiles: " + MiscUtils.getCurrentTime(), false);
            }).start();
        });
        PanelUtils.addComponent(fdf, fdfBtn, Component.RIGHT_ALIGNMENT, 100);

        root.add(fdf);
    }

    /**
     * Initialise les composants pour trouver les fichiers txt non importés (MXF).
     */
    private void missingXmlFiles() {
        JPanel mxf = PanelUtils.createBoxLayoutPanel(BoxLayout.X_AXIS);

        // Label
        JLabel mxfLabel = new JLabel("Rechercher les fichiers XML manquant: ");
        PanelUtils.addComponent(mxf, mxfLabel, Component.LEFT_ALIGNMENT, 700);

        // Bouton d'action
        JButton mxfBtn = ComponentBuilder.buildJButton("Go XML Manquant", 200, Constant.ICON_GO);
        mxfBtn.setToolTipText("Cherche si des fichiers txt n'ont pas d'équivalent XML.");
        mxfBtn.addActionListener((ActionEvent arg0) -> {
            displayText("Start missingXML: " + MiscUtils.getCurrentTime(), false);
            new Thread(() -> {
                fileResult = BatchUtils.missingXML();
                displayText("End missingXML: " + MiscUtils.getCurrentTime(), false);
            }).start();
        });
        PanelUtils.addComponent(mxf, mxfBtn, Component.RIGHT_ALIGNMENT, 100);

        root.add(mxf);
    }

    /**
     * Generates csv reports for a specific year.
     *
     * @param score
     */
    private void topYear() {
        JPanel top = PanelUtils.createBoxLayoutPanel(BoxLayout.X_AXIS);

        // Label
        JLabel topLabel = new JLabel("Générer les tops: ");
        PanelUtils.addComponent(top, topLabel, Component.LEFT_ALIGNMENT, 100);

        String previousYear = String.valueOf(Year.now().getValue() - 1);
        // Year Begin
        JTextField yearBeginTop = new JTextField(previousYear);
        PanelUtils.setSize(yearBeginTop, 100, ComponentBuilder.COMPONENT_HEIGHT);
        PanelUtils.addComponent(top, yearBeginTop, Component.LEFT_ALIGNMENT, 80);

        // Year End
        JTextField yearEndTop = new JTextField(previousYear);
        PanelUtils.setSize(yearEndTop, 100, ComponentBuilder.COMPONENT_HEIGHT);
        PanelUtils.addComponent(top, yearEndTop, Component.LEFT_ALIGNMENT, 80);

        // Album limit
        JTextField albumLimit = new JTextField("10");
        PanelUtils.setSize(albumLimit, 100, ComponentBuilder.COMPONENT_HEIGHT);
        PanelUtils.addComponent(top, albumLimit, Component.LEFT_ALIGNMENT, 80);

        // Song limit
        JTextField songLimit = new JTextField("4");
        PanelUtils.setSize(songLimit, 100, ComponentBuilder.COMPONENT_HEIGHT);
        PanelUtils.addComponent(top, songLimit, Component.LEFT_ALIGNMENT, 80);

        // Checkbox deleted
        JLabel deletedLabel = new JLabel("Supprimés: ");
        JCheckBox deleted = new JCheckBox();
        deleted.setSelected(false);
        PanelUtils.addComponent(top, deletedLabel, Component.LEFT_ALIGNMENT, 0);
        PanelUtils.addComponent(top, deleted, Component.LEFT_ALIGNMENT, 80);

        // Bouton d'action
        JButton topBtn = ComponentBuilder.buildJButton("Go Tops", 200, Constant.ICON_GO);
        topBtn.addActionListener((ActionEvent arg0) -> {
            displayText("Start topYear: " + MiscUtils.getCurrentTime(), false);
            new Thread(() -> {
                fileResult = BatchUtils.topYear(Integer.parseInt(yearBeginTop.getText()),
                        Integer.parseInt(yearEndTop.getText()), Integer.parseInt(albumLimit.getText()),
                        Integer.parseInt(songLimit.getText()), deleted.isSelected());
                displayText("End topYear: " + MiscUtils.getCurrentTime(), false);
            }).start();
        });
        PanelUtils.addComponent(top, topBtn, Component.RIGHT_ALIGNMENT, 100);

        root.add(top);
    }

    private void suspicious() {
        JPanel suspicious = PanelUtils.createBoxLayoutPanel(BoxLayout.X_AXIS);

        // Label
        JLabel suspiciousLabel = new JLabel("Trouver des compositions suspectes: ");
        PanelUtils.addComponent(suspicious, suspiciousLabel, Component.LEFT_ALIGNMENT, 800);

        // suspicious Btn
        JButton suspiciousBtn = ComponentBuilder.buildJButton("Go Compositions Suspectes", 200, Constant.ICON_GO);
        suspiciousBtn.setToolTipText("Trouve des compositions bizarres");
        suspiciousBtn.addActionListener((ActionEvent arg0) -> {
            displayText("Start findSuspiciousComposition: " + MiscUtils.getCurrentTime(), false);
            new Thread(() -> {
                fileResult = BatchUtils.findSuspiciousComposition();
                displayText("End findSuspiciousComposition: " + MiscUtils.getCurrentTime(), false);
            }).start();
        });
        PanelUtils.addComponent(suspicious, suspiciousBtn, Component.RIGHT_ALIGNMENT, 100);

        root.add(suspicious);
    }

    private void slashEdit() {
        JPanel slash = PanelUtils.createBoxLayoutPanel(BoxLayout.X_AXIS);

        // Label
        JLabel suspiciousLabel = new JLabel("Editer les compositions avec un slash: ");
        PanelUtils.addComponent(slash, suspiciousLabel, Component.LEFT_ALIGNMENT, 800);

        // Slash Btn
        JButton slashBtn = ComponentBuilder.buildJButton("Go Compositions Slash", 200, Constant.ICON_GO);
        slashBtn.setToolTipText("Editer des compositions seules contenant un slash dans leur titre");
        slashBtn.addActionListener((ActionEvent arg0) -> {
            displayText("Start slashEdit: " + MiscUtils.getCurrentTime(), false);
            new Thread(() -> {
                fileResult = BatchUtils.slashEdit();
                displayText("End slashEdit: " + MiscUtils.getCurrentTime(), false);
            }).start();
        });
        PanelUtils.addComponent(slash, slashBtn, Component.RIGHT_ALIGNMENT, 100);

        root.add(slash);
    }

    private void duplicateTitle() {
        JPanel duplicateTitle = PanelUtils.createBoxLayoutPanel(BoxLayout.X_AXIS);

        // Label
        JLabel duplicateTitleLabel = new JLabel("Trouver les chansons aux titres identiques: ");
        PanelUtils.addComponent(duplicateTitle, duplicateTitleLabel, Component.LEFT_ALIGNMENT, 800);

        // duplicateTitle Btn
        JButton duplicateTitleBtn = ComponentBuilder.buildJButton("Go Same Title", 200, Constant.ICON_GO);
        duplicateTitleBtn.setToolTipText("Trouve les chansons avec le même titre mais avec l'artiste différent");
        duplicateTitleBtn.addActionListener((ActionEvent arg0) -> {
            displayText("Start findDuplicateTitleComposition: " + MiscUtils.getCurrentTime(), false);
            new Thread(() -> {
                fileResult = BatchUtils.findDuplicateTitleComposition();
                displayText("End findDuplicateTitleComposition: " + MiscUtils.getCurrentTime(), false);
            }).start();
        });
        PanelUtils.addComponent(duplicateTitle, duplicateTitleBtn, Component.RIGHT_ALIGNMENT, 100);

        root.add(duplicateTitle);
    }

    private void validateFileName() {
        JPanel validate = PanelUtils.createBoxLayoutPanel(BoxLayout.X_AXIS);

        // Label
        JLabel validateLabel = new JLabel("Trouver les noms de fichier incorrect: ");
        PanelUtils.addComponent(validate, validateLabel, Component.LEFT_ALIGNMENT, 800);

        // validate Btn
        JButton validateBtn = ComponentBuilder.buildJButton("Go Filename Incorrect", 200, Constant.ICON_GO);
        validateBtn.setToolTipText("Trouve les noms de fichier incorrect");
        validateBtn.addActionListener((ActionEvent arg0) -> {
            displayText("Start findIncorectFileNames: " + MiscUtils.getCurrentTime(), false);
            new Thread(() -> {
                fileResult = BatchUtils.findIncorrectFileNames();
                displayText("End findIncorectFileNames: " + MiscUtils.getCurrentTime(), false);
            }).start();
        });
        PanelUtils.addComponent(validate, validateBtn, Component.RIGHT_ALIGNMENT, 100);

        root.add(validate);
    }

    private void weirdFileSize() {
        JPanel weird = PanelUtils.createBoxLayoutPanel(BoxLayout.X_AXIS);

        // Label
        JLabel weirdLabel = new JLabel("Taille de fichier bizarre: ");
        PanelUtils.addComponent(weird, weirdLabel, Component.LEFT_ALIGNMENT, 800);

        // weird btn
        JButton weirdBtn = ComponentBuilder.buildJButton("Go Weird Size By File", 200, Constant.ICON_GO);
        weirdBtn.setToolTipText("Recherche les fichiers dont la taille est bizarre");
        weirdBtn.addActionListener((ActionEvent arg0) -> {
            displayText("Start weird: " + MiscUtils.getCurrentTime(), false);
            new Thread(() -> {
                fileResult = BatchUtils.weirdFileSize();
                displayText("End weird: " + MiscUtils.getCurrentTime(), false);
            }).start();
        });
        PanelUtils.addComponent(weird, weirdBtn, Component.RIGHT_ALIGNMENT, 100);

        root.add(weird);
    }

    private void averageOfFilesByFiles() {
        JPanel average = PanelUtils.createBoxLayoutPanel(BoxLayout.X_AXIS);

        // Label
        JLabel averageLabel = new JLabel("Moyenne du nombre de fichier par fichier: ");
        PanelUtils.addComponent(average, averageLabel, Component.LEFT_ALIGNMENT, 800);

        // average btn
        JButton averageBtn = ComponentBuilder.buildJButton("Go Average By File", 200, Constant.ICON_GO);
        averageBtn.setToolTipText("Calcule la moyenne du nombre de fichier de chaque composition par fichier.");
        averageBtn.addActionListener((ActionEvent arg0) -> {
            displayText("Start average: " + MiscUtils.getCurrentTime(), false);
            new Thread(() -> {
                fileResult = BatchUtils.averageOfFilesByFiles(this);
                displayText("End average: " + MiscUtils.getCurrentTime(), false);
            }).start();
        });
        PanelUtils.addComponent(average, averageBtn, Component.RIGHT_ALIGNMENT, 100);

        root.add(average);
    }

    private void stats() {
        JPanel stat = PanelUtils.createBoxLayoutPanel(BoxLayout.X_AXIS);

        // Label
        JLabel statLabel = new JLabel("Statistiques: ");
        PanelUtils.addComponent(stat, statLabel, Component.LEFT_ALIGNMENT, 900);

        // Stat btn
        JButton statsBtn = ComponentBuilder.buildJButton("Go Statistiques", 200, Constant.ICON_GO);
        statsBtn.setToolTipText("Génère des statistiques.");
        statsBtn.addActionListener((ActionEvent arg0) -> {
            displayText("Start statistic: " + MiscUtils.getCurrentTime(), false);
            new Thread(() -> {
                fileResult = BatchUtils.stats();
                displayText("End statistic: " + MiscUtils.getCurrentTime(), false);
            }).start();
        });
        PanelUtils.addComponent(stat, statsBtn, Component.RIGHT_ALIGNMENT, 100);

        root.add(stat);
    }

    private void checksIfDeleted() {
        JPanel checks = PanelUtils.createBoxLayoutPanel(BoxLayout.X_AXIS);

        // Label
        JLabel checksLabel = new JLabel("Vérifie si supprimé: ");
        PanelUtils.addComponent(checks, checksLabel, Component.LEFT_ALIGNMENT, 100);

        // File or directory
        JCheckBox isDirectory = (JCheckBox) new ComponentBuilder<Boolean>(JCheckBox.class).withParent(checks)
                .withLabel("Sélectionner un dossier : ").build();

        // File chooser
        JLabel selectedFile = new JLabel();
        FileFilter fileFilter = new FileNameExtensionFilter("txt", "txt");
        JFileChooser chooser = buildFileChooser(checks, "Charge un fichier txt contenant des chansons", selectedFile,
                fileFilter);

        // Type
        JComboBox<RecordType> type = buildTypeComboBox(checks);

        isDirectory.addActionListener((ActionEvent e) -> {
            int selectionMode;
            selectedFile.setText("");
            if (!isDirectory.isSelected()) {
                selectionMode = JFileChooser.FILES_ONLY;
                chooser.setFileFilter(fileFilter);
                chooser.addChoosableFileFilter(fileFilter);
                type.setEnabled(true);
            } else {
                selectionMode = JFileChooser.DIRECTORIES_ONLY;
                chooser.setFileFilter(null);
                chooser.resetChoosableFileFilters();
                type.setEnabled(false);
            }
            chooser.setFileSelectionMode(selectionMode);
        });
        // Bouton d'action
        JButton checksBtn = ComponentBuilder.buildJButton("Go Checks If Deleted", 200, Constant.ICON_GO);
        checksBtn.setToolTipText("Vérifie dans le fichier donné si les compositions sont supprimées");
        checksBtn.addActionListener((ActionEvent arg0) -> {
            if (selectedFile.getText() != null
                    && !StringUtils.equalsIgnoreCase(selectedFile.getText(), Constant.getResourcesDir())) {
                LOG.debug("End browse");
                displayText("Start checksIfDeleted: " + MiscUtils.getCurrentTime(), false);
                new Thread(() -> {
                    BatchUtils.checksIfDeleted(new File(selectedFile.getText()), (RecordType) type.getSelectedItem());
                    displayText("End checksIfDeleted: " + MiscUtils.getCurrentTime(), false);
                }).start();
            } else {
                displayText("No selected file", false);
            }
        });
        PanelUtils.addComponent(checks, checksBtn, Component.RIGHT_ALIGNMENT, 100);

        root.add(checks);
    }

    private static JFileChooser buildFileChooser(JPanel parent, String label, JLabel selectedFile,
            FileFilter fileFilter) {
        JPanel choose = PanelUtils.createBoxLayoutPanel(BoxLayout.Y_AXIS);
        JFileChooser jfile = new JFileChooser(Constant.getResourcesDir());
        jfile.setApproveButtonText("Ouvrir");
        jfile.setPreferredSize(new Dimension(1200, 600));
        jfile.addChoosableFileFilter(fileFilter);
        jfile.setFileFilter(fileFilter);
        JButton browse = ComponentBuilder.buildJButton("Parcourir", 220, Constant.ICON_FOLDER);
        browse.setToolTipText(label);
        browse.addActionListener((ActionEvent arg0) -> {
            LOG.debug("Start browse");
            Optional.ofNullable(jfile.getActionMap().get("viewTypeDetails")).ifPresent(
                    a -> a.actionPerformed(new ActionEvent(arg0.getSource(), arg0.getID(), "viewTypeDetails")));
            if (jfile.showOpenDialog(new JDialog()) == JFileChooser.APPROVE_OPTION) {
                selectedFile.setText(jfile.getSelectedFile().getAbsolutePath());
            }
        });
        PanelUtils.addComponent(choose, browse, Component.LEFT_ALIGNMENT, 0);
        PanelUtils.addComponent(choose, selectedFile, Component.LEFT_ALIGNMENT, 0);
        PanelUtils.addComponent(parent, choose, Component.LEFT_ALIGNMENT, 0);
        return jfile;
    }

    private void findUnknown() {
        JPanel unknown = PanelUtils.createBoxLayoutPanel(BoxLayout.X_AXIS);

        // Label
        JLabel unknownLabel = new JLabel("Inconnu: ");
        PanelUtils.addComponent(unknown, unknownLabel, Component.LEFT_ALIGNMENT, 900);

        // Unknown btn
        JButton unknownsBtn = ComponentBuilder.buildJButton("Go Inconnus", 200, Constant.ICON_GO);
        unknownsBtn.setToolTipText("Trouve le type des compositions inconnues.");
        unknownsBtn.addActionListener((ActionEvent arg0) -> {
            displayText("Start findUnknown: " + MiscUtils.getCurrentTime(), false);
            new Thread(() -> {
                fileResult = BatchUtils.findUnknown();
                displayText("End findUnknown: " + MiscUtils.getCurrentTime(), false);
            }).start();
        });
        PanelUtils.addComponent(unknown, unknownsBtn, Component.RIGHT_ALIGNMENT, 100);

        root.add(unknown);
    }

    /**
     * Initialise la dernière ligne de composant.
     */
    private void lastLine() {
        JPanel lastLine = new JPanel(new GridLayout(0, 2));

        // result
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultLabel = new JTextArea();
        resultLabel.setWrapStyleWord(true);
        resultLabel.setLineWrap(true);
        resultLabel.setOpaque(false);
        resultLabel.setEditable(false);
        resultLabel.setBackground(UIManager.getColor("Label.background"));
        resultLabel.setFont(UIManager.getFont("Label.font"));
        resultLabel.setBorder(UIManager.getBorder("Label.border"));
        resultPanel.add(new JScrollPane(resultLabel), BorderLayout.CENTER);
        lastLine.add(resultPanel);

        // Boutons
        JPanel btnPanel = new JPanel();
        // Clear
        JButton clearBtn = ComponentBuilder.buildJButton("Vider la zone de résultat", 200, Constant.ICON_ERASE);
        clearBtn.addActionListener((ActionEvent arg0) -> {
            resultLabelData = null;
            displayText(null, false);
        });
        btnPanel.add(clearBtn);
        // Notepad
        batchFileBtn = ComponentBuilder.buildJButton("Ouvrir le fichier de résultat", 200, Constant.ICON_TXT_FILE);
        batchFileBtn.addActionListener((ActionEvent arg0) -> openResultFile());
        btnPanel.add(batchFileBtn);

        lastLine.add(btnPanel);
        root.add(lastLine);
    }

    /**
     * Ouvrir le fichier de resultat dans notepad.
     */
    private void openResultFile() {
        LOG.debug("Start openResultFileInNotepad");
        try {
            if (StringUtils.endsWith(fileResult, Constant.CSV_EXTENSION)) {
                FilesUtils.openFileInExcel(fileResult);
            } else {
                FilesUtils.openFileInNotepad(Optional.ofNullable(fileResult).orElse(null), null);
            }
        } catch (MajorException e) {
            displayText(e.toString(), false);
            LOG.error("Erreur lors de l'ouverture du fichier: {}", fileResult, e);
        }
        LOG.debug("End openResultFileInNotepad");
    }

    /**
     * Ajoute un message dans la zone de texte resultLabel.
     *
     * @param text        un nouveau texte à afficher
     * @param replaceLast true replace previous line, false append it
     */
    public void displayText(String text, boolean replaceLast) {
        if (resultLabelData == null) {
            resultLabelData = new ArrayList<>();
        }
        if (!replaceLast) {
            resultLabelData.add(text);
        } else {
            resultLabelData.set(resultLabelData.size() - 1, text);
        }
        resultLabel.setText(StringUtils.join(resultLabelData, Constant.NEW_LINE));
        resultLabel.setForeground(new Color(243, 16, 16));
        Font labelFont = resultLabel.getFont();
        resultLabel.setFont(new Font(labelFont.getName(), labelFont.getStyle(), 20));
    }

    public JButton getBatchFileBtn() {
        return batchFileBtn;
    }

}
