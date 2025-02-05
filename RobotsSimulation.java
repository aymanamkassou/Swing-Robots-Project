import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Random;

/*
 * Classe principale pour lancer l'application.
 * Cette classe cree la fenetre Swing, la grille, le panel d'affichage et les boutons de controle.
 */
public class RobotsSimulation {

    // Liste pour stocker les robots actifs
    private static ArrayList<Robot> robotList = new ArrayList<>();
    // Tableau de couleurs pour les robots (suffisamment de couleurs)
    private static final Color[] COLORS = {
        Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.MAGENTA,
        Color.CYAN, Color.YELLOW, Color.PINK, Color.GRAY, Color.LIGHT_GRAY,
        new Color(128, 0, 128), new Color(0, 128, 128)  // couleurs personnalisees
    };

    // Grille de la simulation
    private static Grid grid;
    // Panel pour afficher la grille et les robots
    private static RobotPanel robotPanel;

    /*
     * Methode main pour lancer l'application.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }

    /*
     * Methode pour creer et afficher la fenetre principale.
     */
    private static void createAndShowGUI() {
        // Creation de la fenetre principale
        JFrame frame = new JFrame("Simulation de Robots");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 800);
        frame.setLayout(new BorderLayout());

        // Creation de la grille (25x25)
        grid = new Grid(25, 25);
        // Creation du panel d'affichage
        robotPanel = new RobotPanel(grid);
        frame.add(robotPanel, BorderLayout.CENTER);

        // Creation du panel de controle pour les boutons
        JPanel controlPanel = new JPanel();
        // Bouton pour ajouter un robot
        JButton addButton = new JButton("Ajouter Robot");
        // Bouton pour supprimer un robot
        JButton removeButton = new JButton("Supprimer Robot");

        controlPanel.add(addButton);
        controlPanel.add(removeButton);
        frame.add(controlPanel, BorderLayout.SOUTH);

        // Action lors du clic sur le bouton "Ajouter Robot"
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Si le nombre de robots est egal au nombre total de cases, la grille est pleine
                if (robotList.size() >= grid.getRows() * grid.getColumns()) {
                    JOptionPane.showMessageDialog(frame, "Erreur : La grille est pleine !", "Erreur", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                // Recherche d'une case libre aleatoire
                Random rand = new Random();
                int x, y;
                do {
                    x = rand.nextInt(grid.getColumns());
                    y = rand.nextInt(grid.getRows());
                } while (grid.getRobotAt(x, y) != null);
                // Choix de la couleur en fonction du nombre de robots deja ajoutes
                Color color = COLORS[robotList.size() % COLORS.length];
                // Creation du robot
                Robot robot = new Robot(x, y, color, grid, robotPanel);
                // Ajout du robot dans la grille
                grid.addRobot(robot, x, y);
                // Ajout a la liste et demarrage du thread
                robotList.add(robot);
                robot.start();
                // Actualisation de l'affichage
                robotPanel.repaint();
            }
        });

        // Action lors du clic sur le bouton "Supprimer Robot"
        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (robotList.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Erreur : Aucun robot a supprimer !", "Erreur", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                // Suppression du dernier robot de la liste
                Robot robot = robotList.remove(robotList.size() - 1);
                // Arret du thread du robot
                robot.stopRobot();
                // Suppression du robot de la grille
                grid.removeRobot(robot);
                // Actualisation de l'affichage
                robotPanel.repaint();
            }
        });

        frame.setVisible(true);

        // Optionnel : creation initiale de 5 robots
        for (int i = 0; i < 5; i++) {
            addButton.doClick();
        }
    }
}

/*
 * Classe Grid qui gere la grille et la synchronisation des deplacements.
 */
class Grid {
    private int rows;
    private int columns;
    // Matrice representant la grille; chaque case contient null ou une reference vers un Robot
    private Robot[][] cells;

    /*
     * Constructeur de la grille.
     * @param rows nombre de lignes
     * @param columns nombre de colonnes
     */
    public Grid(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        cells = new Robot[rows][columns];
    }

    /*
     * Methode synchronisee pour ajouter un robot a une case donnee.
     * @param robot le robot a ajouter
     * @param x position en x (colonne)
     * @param y position en y (ligne)
     * @return vrai si la case etait libre et le robot est ajoute, faux sinon.
     */
    public synchronized boolean addRobot(Robot robot, int x, int y) {
        if (!inBounds(x, y))
            return false;
        if (cells[y][x] == null) {
            cells[y][x] = robot;
            return true;
        }
        return false;
    }

    /*
     * Methode synchronisee pour deplacer un robot vers une nouvelle case.
     * Cette methode verifie que la nouvelle case est dans les limites et libre.
     * @param robot le robot a deplacer
     * @param newX nouvelle position en x (colonne)
     * @param newY nouvelle position en y (ligne)
     * @return vrai si le deplacement a reussi, faux sinon.
     */
    public synchronized boolean moveRobot(Robot robot, int newX, int newY) {
        if (!inBounds(newX, newY))
            return false;
        if (cells[newY][newX] != null)
            return false;
        // Supprime le robot de son ancienne case
        cells[robot.getY()][robot.getX()] = null;
        // Met a jour la position du robot
        robot.setX(newX);
        robot.setY(newY);
        // Place le robot dans la nouvelle case
        cells[newY][newX] = robot;
        return true;
    }

    /*
     * Methode synchronisee pour supprimer un robot de la grille.
     * @param robot le robot a supprimer
     */
    public synchronized void removeRobot(Robot robot) {
        int x = robot.getX();
        int y = robot.getY();
        if (inBounds(x, y) && cells[y][x] == robot) {
            cells[y][x] = null;
        }
    }

    /*
     * Methode pour verifier si les coordonnees sont dans la grille.
     * @param x position en x
     * @param y position en y
     * @return vrai si dans les limites, faux sinon.
     */
    public boolean inBounds(int x, int y) {
        return x >= 0 && x < columns && y >= 0 && y < rows;
    }

    /*
     * Accesseur pour le nombre de lignes.
     */
    public int getRows() {
        return rows;
    }

    /*
     * Accesseur pour le nombre de colonnes.
     */
    public int getColumns() {
        return columns;
    }

    /*
     * Methode synchronisee pour obtenir le robot present a une case donnee.
     * @param x position en x
     * @param y position en y
     * @return le robot present ou null.
     */
    public synchronized Robot getRobotAt(int x, int y) {
        if (inBounds(x, y))
            return cells[y][x];
        return null;
    }
}

/*
 * Classe Robot qui herite de Thread et represente un robot se deplacant dans la grille.
 */
class Robot extends Thread {
    private int x;
    private int y;
    private Color color;
    private Grid grid;
    private RobotPanel panel;
    private Random rand;
    // Booleen pour controler l'arret du thread
    private volatile boolean active = true;

    /*
     * Constructeur du robot.
     * @param x position initiale en x
     * @param y position initiale en y
     * @param color couleur du robot
     * @param grid reference vers la grille
     * @param panel reference vers le panel d'affichage
     */
    public Robot(int x, int y, Color color, Grid grid, RobotPanel panel) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.grid = grid;
        this.panel = panel;
        this.rand = new Random();
    }

    // Accesseurs pour la position et la couleur
    public int getX() { return x; }
    public int getY() { return y; }
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
    public Color getColor() { return color; }

    /*
     * Methode run qui fait deplacer le robot de maniere aleatoire.
     * Le robot choisit une direction, tente de se deplacer et fait une pause.
     */
    public void run() {
        while (active) {
            // Choix d'une direction aleatoire
            int direction = rand.nextInt(4); // 0: haut, 1: droite, 2: bas, 3: gauche
            int newX = x;
            int newY = y;
            switch (direction) {
                case 0:
                    newY = y - 1; // haut
                    break;
                case 1:
                    newX = x + 1; // droite
                    break;
                case 2:
                    newY = y + 1; // bas
                    break;
                case 3:
                    newX = x - 1; // gauche
                    break;
            }
            // Tente de deplacer le robot dans la grille
            boolean moved = grid.moveRobot(this, newX, newY);
            if (moved) {
                // Redessine le panel pour afficher le deplacement
                panel.repaint();
            }
            try {
                // Pause aleatoire pour ralentir le deplacement
                Thread.sleep(200 + rand.nextInt(300));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * Methode pour arreter le thread du robot.
     */
    public void stopRobot() {
        active = false;
    }
}

/*
 * Classe RobotPanel qui herite de JPanel et affiche la grille et les robots.
 */
class RobotPanel extends JPanel {
    private Grid grid;

    /*
     * Constructeur du panel.
     * @param grid reference vers la grille contenant les robots
     */
    public RobotPanel(Grid grid) {
        this.grid = grid;
    }

    /*
     * Redefinition de la methode paintComponent pour dessiner la grille et les robots.
     */
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int rows = grid.getRows();
        int columns = grid.getColumns();
        int cellWidth = getWidth() / columns;
        int cellHeight = getHeight() / rows;

        // Dessine la grille en lignes grises
        g.setColor(Color.LIGHT_GRAY);
        for (int i = 0; i <= columns; i++) {
            g.drawLine(i * cellWidth, 0, i * cellWidth, getHeight());
        }
        for (int i = 0; i <= rows; i++) {
            g.drawLine(0, i * cellHeight, getWidth(), i * cellHeight);
        }

        // Dessine les robots sur la grille
        // La synchronisation permet d'eviter des problemes lors de l'acces a la grille
        synchronized (grid) {
            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < columns; x++) {
                    Robot robot = grid.getRobotAt(x, y);
                    if (robot != null) {
                        g.setColor(robot.getColor());
                        g.fillRect(x * cellWidth, y * cellHeight, cellWidth, cellHeight);
                    }
                }
            }
        }
    }
}