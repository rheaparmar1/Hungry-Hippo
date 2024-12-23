/**
Rhea Parmar
ISU: Hungry Hippos
1/25/2022

This program, using graphics, allows users to play Hungry Hippos(tm)
*/
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

import javax.sound.sampled.*;
import javax.swing.*;
@SuppressWarnings("serial")
public class hungryHippo_beautified extends JPanel implements Runnable, ActionListener, MouseListener, KeyListener, ItemListener {
	static JFrame frame;
	static JFrame aboutFrame;
	
	//Final ints
	final int SQUARE_SIZE = 30;
	final int BORDER_SIZE = 30;
	final int EMPTY = 0;
	final int FRUIT = 1;
	final int BOMB = 2;
	final int WALL = 3;
	final int WIN_MINIMUM = 5;
	
	boolean gameOver=false;
	boolean hitBomb = false;
	int level = 1;
	int pointCount =0;
	int coverage =0;

	//Piece location storage - Bomb, fruit, walls, hippo
	Rectangle rect = new Rectangle(30, 330, 30, 30);

	int [][] board;
	Rectangle [] wallRectArr;
	Rectangle fruitRect=new Rectangle(SQUARE_SIZE,SQUARE_SIZE);
	Rectangle [] bombRectArr=new Rectangle[5];
	
	//Graphic variables
	Image hippoImage, bombImage, fruitImage, wallImage;
	Clip backgroundAudio, fruitAudio, bombAudio, borderAudio;
	int screenWidth = 660; 
	int screenHeight = 660;
	JCheckBoxMenuItem musicMute, soundMute;
	boolean musicOn = true;
	boolean soundOn = true;
	boolean up, down, left, right;
	int speed = 2;
	Thread thread;
	int FPS = 40;
	int sleepDuration = 9;
	Image offScreenImage;
	Graphics offScreenBuffer;

	//Colours
	private final Color white = new Color (255,255,255);
	private final Color darkBlue = new Color(101,150,193);
	private final Color lightBlue = new Color (208,230,244);
	private final Color darkDarkBlue = new Color (135,134,138);

	//----------------------------------------------------------------
	// CONSTRUCTOR	
	public hungryHippo_beautified() {
		//Sets defaults for frame
		setPreferredSize(new Dimension(660, 750));
		setLocation (100, 10);
		setBackground (white);
		setLayout (new BoxLayout (this, BoxLayout.PAGE_AXIS));

		//initializes major arrays (board[][], wallRectArr[], bombRectArr[])
		board = new int [22] [22];
		initialize();
		for (int i=0; i<5;i++) {
			bombRectArr[i] = new Rectangle(SQUARE_SIZE,SQUARE_SIZE);
		}
		thread = new Thread(this);
		thread.start();
		// Menu Setup
		//Game menu option
		JMenu gameMenu = new JMenu ("Game");
		JMenuItem newOption = new JMenuItem ("New Game (N)");
		newOption.setActionCommand ("newGame");
		newOption.addActionListener (this);
		JMenuItem levelOption = new JMenuItem ("Restart Level (R)");
		levelOption.setActionCommand ("restartLevel");
		levelOption.addActionListener (this);
		JMenuItem exitOption = new JMenuItem ("Exit (E)");
		exitOption.setActionCommand ("exitGame");
		exitOption.addActionListener (this);

		gameMenu.add(newOption);
		gameMenu.addSeparator ();
		gameMenu.add (levelOption);
		gameMenu.addSeparator ();
		gameMenu.add (exitOption);

		//Level menu option
		JMenu levelMenu = new JMenu ("Level");
		JMenuItem level1 = new JMenuItem ("Level 1");
		level1.setActionCommand ("level1");
		level1.addActionListener (this);
		JMenuItem level2 = new JMenuItem ("Level 2");
		level2.setActionCommand ("level2");
		level2.addActionListener (this);
		JMenuItem level3 = new JMenuItem ("Level 3");
		level3.setActionCommand ("level3");
		level3.addActionListener (this);

		levelMenu.add(level1);
		levelMenu.addSeparator ();
		levelMenu.add(level2);
		levelMenu.addSeparator ();
		levelMenu.add(level3);

		//Setting menu option
		JMenu SettingsMenu = new JMenu ("Settings");
		musicMute = new JCheckBoxMenuItem ("Music", true);
		musicMute.setActionCommand ("musicMute");
		musicMute.addActionListener (this);
		soundMute = new JCheckBoxMenuItem ("Sound Effects",true);
		soundMute.setActionCommand ("soundMute");
		soundMute.addActionListener (this);

		SettingsMenu.add(musicMute);
		SettingsMenu.addSeparator ();
		SettingsMenu.add(soundMute);
		
		//About Menu
		JMenu aboutMenu = new JMenu ("About");
		JMenuItem instructions = new JMenuItem ("Instructions");
		instructions.setActionCommand ("about");
		instructions.addActionListener (this);
		
		aboutMenu.add(instructions);

		//Add Menus to Frame
		JMenuBar mainMenu = new JMenuBar ();
		mainMenu.add (gameMenu);
		mainMenu.add (levelMenu);
		mainMenu.add (SettingsMenu);
		mainMenu.add (aboutMenu);

		frame.setJMenuBar (mainMenu);
		
		//Images loading
		MediaTracker tracker = new MediaTracker (this);
		hippoImage = Toolkit.getDefaultToolkit ().getImage ("hippo.png");
		tracker.addImage (hippoImage, 0);
		wallImage = Toolkit.getDefaultToolkit ().getImage ("sand.jpg");
		tracker.addImage (wallImage, 1);
		bombImage = Toolkit.getDefaultToolkit ().getImage ("bomb.png");
		tracker.addImage (hippoImage, 2);
		fruitImage = Toolkit.getDefaultToolkit ().getImage ("strawberry.gif");
		tracker.addImage (hippoImage, 3);

		try
		{
			tracker.waitForAll ();
		}
		catch (InterruptedException e)
		{
		}

		frame.setIconImage (hippoImage);
		
		//Sets up board
		placeBomb();
		placeFruit();
		setFocusable (true); 
		addKeyListener (this);
		addMouseListener (this);

		//Background music and sound effects
		try {

			AudioInputStream sound = AudioSystem.getAudioInputStream(new File ("MonkeysSpinningMonkeys.wav"));
			backgroundAudio = AudioSystem.getClip();
			backgroundAudio.open(sound);

			sound = AudioSystem.getAudioInputStream(new File ("explode.wav"));
			bombAudio = AudioSystem.getClip();
			bombAudio.open(sound);

			sound = AudioSystem.getAudioInputStream(new File ("RobloxDeathSound.wav"));
			borderAudio = AudioSystem.getClip();
			borderAudio.open(sound);

			sound = AudioSystem.getAudioInputStream(new File ("slip.wav"));
			fruitAudio = AudioSystem.getClip();
			fruitAudio.open(sound);

		} 
		catch (Exception e) {
		}
		
		//Set background music in loop
		backgroundAudio.setFramePosition (0); //<-- play sound file again from beginning
		backgroundAudio.loop(Clip.LOOP_CONTINUOUSLY);
	}
	// METHODS ----------------------------------------------------------------
	/** 
	 * Description: this method initializes wall array and sets coverage based on level
	 * Parameters: N/A
	 * Return: N/A
	 */
	public void initialize() {
		coverage=0;
		if (level ==2) 
			coverage = 20;
		else if (level ==3)
			coverage = 40;
		wallRectArr=new Rectangle[coverage];

		for (int i=0; i<coverage;i++) {
			wallRectArr[i] = new Rectangle();
		}
	}
	
	/** 
	 * Description: this method randomly chooses a location for the 5 bombs.
	 * Parameters: N/A
	 * Return: N/A
	 */
	public void  placeBomb() {
		for (int i=0; i<5;i++) {
			int x = (int)(Math.random()*(20))+1;
			int y = (int)(Math.random()*(20))+1;
			if (board [x][y]==EMPTY && x!=11 && y!=1) {
				board [x][y] = BOMB;	
				bombRectArr[i].setLocation(y*30,x*30);
			}
			else
				i--;
		}
	}

	/** 
	 * Description: this method randomly chooses a location for the fruit.
	 * Parameters: N/A
	 * Return: N/A
	 */
	public void placeFruit() {
		boolean unique = false;
		while(unique==false) {
			int x = (int)(Math.random()*(18))+2;
			int y = (int)(Math.random()*(18))+2;
			if(board [x][y]==EMPTY && x!=11 && y!=1) {
				board [x][y] = FRUIT;
				fruitRect.setLocation(y*30,x*30); 
				unique=true;
			}
		}
	}
	
	/** 
	 * Description: this method randomly creates locations for a preset number of walls (based on level), 
	 * and checks if wall location makes surrounding fruit impossible to get.
	 * Parameters: N/A
	 * Return: N/A
	 */
	public void setWalls() {
		for (int wallCount=0; wallCount<coverage; wallCount++) {
			boolean impossible =true;
			while (impossible ==true) {
				int row = (int)(Math.random()*(17)+2);
				int col = (int)(Math.random()*(17)+2);

				//check if layout is impossible, criteria:
					// 1. allow no more than one diagonal 
					// 2. max 2 surrounding
					// 3. not allowed on outer two rows/columns
				
				if (board[row][col]==EMPTY && row!=11 && col!=1) {	
					int inContact = 0;
					int diagonalInComparison = 0;
					int overallCount = 0;
					for (int rowCount = row-1; rowCount<=row+1; rowCount++) {
						for (int colCount = col-1; colCount<=col+1; colCount++) {
							overallCount++;

							if (rowCount!=row&&colCount!=col) {
								//in contact check
								if (board[rowCount][colCount]==WALL) {
									inContact++;
									//if diagonal in comparison check
									if (overallCount%2==1)
										diagonalInComparison++;
								}
							}
						}
					}
					//checks if no more than 2 contacts including 1 diagonal
					if (inContact<=2&&diagonalInComparison<=1) {
						impossible=false;
						board[row][col] =WALL;
						wallRectArr[wallCount].x= col*30;
						wallRectArr[wallCount].y= row *30;
						wallRectArr[wallCount].width= SQUARE_SIZE;
						wallRectArr[wallCount].height= SQUARE_SIZE;
					}
					else
						wallCount--;
					//	System.out.println("Wall location: "+wallRectArr[wallCount].x+", "+wallRectArr[wallCount].y+" loop #: "+wallCount+" Success: "+board[row][col]);
				}
			}
		}
	}
	
	/** 
	 * Description: this method checks if bounds, bombs, fruits or walls are hit.
	 * Parameters: N/A
	 * Return: N/A
	 */
	public void checkStatus() {

		//Bounds check
		if(rect.x < 30 || rect.x > screenWidth - rect.width-SQUARE_SIZE) {
			gameOver=true;
			//plays border audio
			if(soundOn==true) {
				borderAudio.setFramePosition (30000); //<-- play sound file again from beginning
				borderAudio.start ();
			}
		}
		if(rect.y < 30||rect.y > screenHeight - rect.height-SQUARE_SIZE) {
			//plays border audio
			if (soundOn==true) {
				borderAudio.setFramePosition (30000); //<-- play sound file again from beginning
				borderAudio.start ();
			}
			gameOver=true;
		}
		
		//Bomb check
		for (int i=0;i<5;i++) {
			if(rect.intersects(bombRectArr[i])){
				//Moves hippo halfway of square to look smoother
				if(left==true) {
					rect.x-=15;
				}	
				else if(right==true) {
					rect.x+=15;
				}
				else if(up==true) {
					rect.y-=15;
				}
				else if(down==true) {
					rect.y+=15;
				}

				//Play bomb audio
				if (soundOn==true) {
					bombAudio.setFramePosition (30000); //<-- play sound file again from beginning
					bombAudio.start ();
				}
				gameOver=true;
				hitBomb = true;
			}
		}
		
		//Fruit check
		if(rect.intersects(fruitRect)){
			//Moves hippo halfway of square to look smoother
			if(left==true) {
				rect.x-=15;
			}	
			else if(right==true) {
				rect.x+=15;
			}
			else if(up==true) {
				rect.y-=15;
			}
			else if(down==true) {
				rect.y+=15;
			}

			pointCount++;
			
			//play fruit audio
			if (soundOn==true) {
				fruitAudio.setFramePosition (8300); //<-- play sound file again from beginning
				fruitAudio.start ();
			}
			//resets board square to allow other pieces to be generated in this position in the future
			board[(rect.y)/30][(rect.x)/30]=EMPTY;
			if (pointCount==WIN_MINIMUM) {
				gameOver=true;
				return;
			}
			
			//calls placeFruit() to set new fruit location
			placeFruit();
		}


		//Wall check for every level, but level 1
		if (level !=1)	{
			//loop through wallRectArr to access Rectangles individually
			for(int i = 0; i < wallRectArr.length; i++) {
				if (rect.intersects(wallRectArr[i])){
					
					//finds rectangle corner coordinates
					double left1 = rect.getX();
					double right1 = rect.getX() + rect.getWidth();
					double top1 = rect.getY();
					double bottom1 = rect.getY() + rect.getHeight();
					double left2 = wallRectArr[i].getX();
					double right2 = wallRectArr[i].getX() + wallRectArr[i].getWidth();
					double top2 = wallRectArr[i].getY();
					double bottom2 = wallRectArr[i].getY() + wallRectArr[i].getHeight();

					if(right1 > left2 && 
							left1 < left2 && 
							right1 - left2 < bottom1 - top2 && 
							right1 - left2 < bottom2 - top1)
					{
						//rect collides from left side of the wall
						rect.x = wallRectArr[i].x - rect.width;
					}
					else if(left1 < right2 &&
							right1 > right2 && 
							right2 - left1 < bottom1 - top2 && 
							right2 - left1 < bottom2 - top1)
					{
						//rect collides from right side of the wall
						rect.x = wallRectArr[i].x + wallRectArr[i].width;
					}
					else if(bottom1 > top2 && top1 < top2)
					{
						//rect collides from top side of the wall
						rect.y = wallRectArr[i].y - rect.height;
					}
					else if(top1 < bottom2 && bottom1 > bottom2)
					{
						//rect collides from bottom side of the wall
						rect.y = wallRectArr[i].y + wallRectArr[i].height;
					}
				}
			}
		}
	}

	/** 
	 * Description: this method resets board to origional layout-resetting major variables.
	 * Parameters: N/A
	 * Return: N/A
	 */
	public void resetBoard() {
		board = new int [22] [22];
		
		//sets Thread.sleep variable duration
		if (level==1)
			sleepDuration =15;
		else if (level==2)
			sleepDuration =9;
		else if (level==3)
			sleepDuration =5;
		
		left = false;
		right = false;
		up = false;
		down = false;
		
		//places rect (which hippo is on) back to origional location
		rect.x=30;
		rect.y=330;
		gameOver = false;
		pointCount=0;
		
		//set walls, bombs, fruit
		if (level !=1) {
			initialize();
			setWalls();
		}
		placeBomb();
		placeFruit();

		repaint ();
	}
	
	/** 
	 * Description: this method resets game to first level
	 * Parameters: N/A
	 * Return: N/A
	 */
	public void newGame() {
		level = 1;
		resetBoard();

	}
	
	/** 
	 * Description: this method changes rect (hippo) location based on speed. It also 
	 * auto-rounds rect location to keep piece on an exact grid square.
	 * Parameters: N/A
	 * Return: N/A
	 */
	void move() {
		if(left) {
			if (rect.y%30!=0) {
				rect.y=rect.y+30-(rect.y%30);
			}
			rect.x -= speed;
		}	
		else if(right) {
			if (rect.y%30!=0) {
				rect.y=rect.y+30-(rect.y%30);
			}
			rect.x += speed;
		}
		else if(up) {
			if (rect.x%30!=0) {
				rect.x=rect.x+30-(rect.x%30);
			}
			rect.y += -speed;
		}
		else if(down) {
			if (rect.x%30!=0) {
				rect.x=rect.x+30-(rect.x%30);
			}
			rect.y += speed;
		}

		checkStatus();
	}
	
	@Override
	public void run() {
		while (true) {	
			while(gameOver==false) {
				//call move to change position of rect (hippo piece)
					move();
					if (gameOver==true)
						break;
			
				//repaint using most recent changes to locations
					this.repaint();	
					
				//Thread.sleep to slow down speed
					try {
						Thread.sleep(sleepDuration);
					} catch(Exception e) {
						e.printStackTrace();
					}
			}
			
			//Option pane if bomb is hit
			if (gameOver==true && hitBomb==true) {
				resetBoard();
				hitBomb = false;

				JOptionPane.showMessageDialog (this, "Bomb hit! You Lost!",
						"Game Over", JOptionPane.WARNING_MESSAGE);
			}
			
			//Option pane if border is hit
			else if (gameOver==true && pointCount<WIN_MINIMUM) {
				resetBoard();

				JOptionPane.showMessageDialog (this, "Border hit! You Lost!",
						"Game Over", JOptionPane.WARNING_MESSAGE);
			}
			
			//Option pane if player wins (collects minimum # of point required to win)
			else if (pointCount== WIN_MINIMUM) {
				
				gameOver=true;
				//Resets board to next level
				if (level<3) {
					level++;
					resetBoard();
				}
				
				JOptionPane.showMessageDialog (this, "You Won!",
						"Game Over", JOptionPane.WARNING_MESSAGE);
				return;
			}
		}
	}

	//----------------------------------------------------------------

	public void actionPerformed (ActionEvent event ) {
		String eventName = event.getActionCommand ();
		boolean soundState = soundMute.getState();
		boolean musicState = musicMute.getState();

		if (eventName.equals ("newGame"))
		{
			newGame ();
		}
		else if (eventName.equals ("restartLevel"))
		{
			resetBoard();
		}
		else if (eventName.equals ("exitGame"))
		{
			System.exit (0);
		}
		else if (eventName.equals ("level1"))
		{
			//sets game to level 1
			level = 1;
			resetBoard();
		}
		else if (eventName.equals ("level2"))
		{
			//sets game to level 2
			level = 2;	
			resetBoard();
		}
		else if (eventName.equals ("level3"))
		{
			//sets game to level 3
			level = 3;		
			resetBoard();
		}
		else if (eventName.equals ("musicMute"))
		{
			//stop/start background music
			if (musicState==false) {
				backgroundAudio.stop();
			}
			else {
				backgroundAudio.setFramePosition (0); //<-- play sound file again from beginning
				backgroundAudio.loop(Clip.LOOP_CONTINUOUSLY);
			}
		}
		else if (eventName.equals ("soundMute"))
		{
			//stop/start sound effects
			if (soundState==false)
				soundOn =false;
			else
				soundOn =true;
		}
		else if (eventName.equals ("about"))
		{
			// create a new frame 
			aboutFrame = new JFrame("label");
			aboutFrame.setLocation (155, 55);
			aboutFrame.setSize(300, 300);
			aboutFrame.setResizable(false);
	        // create a label to display text
	        JLabel nameLabel = new JLabel();
	        JLabel instructions = new JLabel();
	        JLabel listed = new JLabel();

	        // add text to label     
	        nameLabel.setText("<html><BR><BR><BR>Hungry Hippos™  created by Rhea Parmar</html>");
	        instructions.setText ("<html><centre>Instructions</centre></html>");
	        listed.setText("<html>Guide your game piece (the hippo) with the<BR>"
	        			+ " arrow keys to the fruit. Each fruit collected <BR>"
	        			+ "results in a increases of your points. Achieve<BR> "
	        			+ "a total of 5 points to move on to the next level.<BR>"
	        			+ " Make sure you avoid hitting bombs or the game <BR>"
	        			+ "border. Doing so results in a loss. Ensure to <BR>"
	        			+ "maneuver around sand patches. Though if used <BR>"
	        			+ "strategically can be used to slow down the game"
	        			+ "<BR> during difficult levels.</html>");
	        // create a panel
	        JPanel panel = new JPanel();
	 
	        // add label to panel
	     
	        panel.add(instructions);
	        panel.add(listed);
	        panel.add(nameLabel);
	        
	        // add panel to frame
	        aboutFrame.add(panel);	      
	 
	        aboutFrame.show();
		}
	}

	//----------------------------------------------------------------
	//PAINT COMPONENT
	public void paintComponent (Graphics g)
	{
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		// Set up the offscreen buffer the first time paint() is called
		if (offScreenBuffer == null)
		{
			offScreenImage = createImage (this.getWidth (), this.getHeight ());
			offScreenBuffer = offScreenImage.getGraphics ();
		}

		// All of the drawing is done to an off screen buffer which is
		// then copied to the screen.  This will prevent flickering
		// Clear the offScreenBuffer first
		offScreenBuffer.clearRect (0, 0, this.getWidth (), this.getHeight ());
		
		//Draw rectangles for walls
		if (level!=1) {
			for (int i =0; i<coverage;i++) {
				offScreenBuffer.setColor(Color.WHITE);
				offScreenBuffer.fillRect(wallRectArr[i].x,wallRectArr[i].y,SQUARE_SIZE, SQUARE_SIZE);
			}
		}
		//Draw rectangle for fruit
		offScreenBuffer.setColor(white);
		offScreenBuffer.fillRect(fruitRect.x,fruitRect.y,SQUARE_SIZE, SQUARE_SIZE);
		
		//Draw rectangles for bombs
		for (int i =0; i<5;i++) {
			offScreenBuffer.fillRect(bombRectArr[i].x,bombRectArr[i].y,SQUARE_SIZE, SQUARE_SIZE);
		}
		
		//Draw rectangle for hippo
		offScreenBuffer.fillRect(rect.x,rect.y, SQUARE_SIZE,SQUARE_SIZE);
	
		// Redraw the board grid
				int pieceCount =0; //<-- to track grid # to alternate grid colour
		for (int row = 1 ; row <= 20 ; row++,pieceCount++)
			for (int column = 1 ; column <= 20 ; column++, pieceCount++)
			{
				// Find the x and y positions for each row and column
				int xPos = (column - 1) * SQUARE_SIZE + BORDER_SIZE;
				int yPos = row * SQUARE_SIZE;

				// Draw the squares with alternating colours
				if(pieceCount%2==0) 
					offScreenBuffer.setColor (lightBlue);
				else
					offScreenBuffer.setColor (darkBlue);
				offScreenBuffer.fillRect (xPos, yPos, SQUARE_SIZE, SQUARE_SIZE);

			}
		
		//draw fruit image
		offScreenBuffer.drawImage(fruitImage, fruitRect.x,fruitRect.y,SQUARE_SIZE, SQUARE_SIZE,this );
		
		//draw wall image
		if (level!=1) {
			for (int i =0; i<coverage;i++) {
				offScreenBuffer.drawImage(wallImage, wallRectArr[i].x,wallRectArr[i].y,SQUARE_SIZE, SQUARE_SIZE,this);
				offScreenBuffer.setColor(Color.WHITE);
				offScreenBuffer.drawRect(wallRectArr[i].x,wallRectArr[i].y,SQUARE_SIZE, SQUARE_SIZE);
			}
		}
		//draw bomb image
		for (int i =0; i<5;i++) {
			offScreenBuffer.drawImage(bombImage, bombRectArr[i].x,bombRectArr[i].y,SQUARE_SIZE, SQUARE_SIZE,this );

		}
		
		//draw hippo image
		offScreenBuffer.drawImage(hippoImage, rect.x,rect.y,SQUARE_SIZE, SQUARE_SIZE,this );

		// Transfer the offScreenBuffer to the screen
		g.drawImage (offScreenImage, 0, 0, this);
		
		// Score box
		g.setColor(lightBlue);
		g.fillRect(30, 645, 150, 90);
		g.setColor(darkBlue);
		g.setFont(new Font("Arial Black", Font.PLAIN, 30)); 
		g.drawString("LVL: "+level, 45, 680);
		g.drawString("PTS: "+pointCount, 45, 720);
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void keyPressed(KeyEvent e) {
		int key = e.getKeyCode();
		//Direction keys - sets appropriate keys and resets all others
		if(key == KeyEvent.VK_LEFT||key == KeyEvent.VK_A) {
			left = true;
			right = false;
			up = false;
			down = false;
		}else if(key == KeyEvent.VK_RIGHT||key == KeyEvent.VK_D) {
			right = true;
			left = false;
			up = false;
			down = false;
		}if(key == KeyEvent.VK_UP||key == KeyEvent.VK_W) {
			up = true;
			down = false;
			left = false;
			right = false;
		}else if(key == KeyEvent.VK_DOWN||key == KeyEvent.VK_S) {
			down = true;
			up = false;
			left = false;
			right = false;
		}else if(key == KeyEvent.VK_N) {
			newGame();
		}else if(key == KeyEvent.VK_R) {
			resetBoard();
		}else if(key == KeyEvent.VK_E) {
			System.exit(0);
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub

	}


	public static void main(String[] args) {
		//New frame
		frame = new JFrame ("Hungry Hippo");
		hungryHippo_beautified myPanel = new hungryHippo_beautified ();
		frame.addKeyListener(myPanel);
		frame.add (myPanel);
		frame.pack ();
		frame.setVisible (true);
		frame.setResizable(false);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
}
