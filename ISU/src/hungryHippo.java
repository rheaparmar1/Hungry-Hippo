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
public class hungryHippo extends JPanel implements Runnable, ActionListener, MouseListener, KeyListener, ItemListener {
	static JFrame frame;
	
	
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
	
	Rectangle rect = new Rectangle(30, 330, 30, 30);
	
	int [][] board;
	Rectangle [] wallRectArr;
	Rectangle fruitRect=new Rectangle(SQUARE_SIZE,SQUARE_SIZE);
	Rectangle [] bombRectArr=new Rectangle[5];

	Image hippoImage, bombImage, fruitImage;
	Clip backgroundAudio, fruitAudio, bombAudio, borderAudio;
	int screenWidth = 660; 
	int screenHeight = 660;
	JCheckBoxMenuItem musicMute, soundMute;
	boolean musicOn =true;
	boolean soundOn=true;
	boolean up, down, left, right;
	int speed = 1;
	Thread thread;
	int FPS = 40;
	int sleepDuration = 9;
	Image offScreenImage;
	Graphics offScreenBuffer;

	private final Color offWhite = new Color (200,200,200);


	private int frameCoint;

	//----------------------------------------------------------------
	//CONSTRUCTOR	
	public hungryHippo() {

		setPreferredSize(new Dimension(20 * SQUARE_SIZE + 2 * BORDER_SIZE + 1, (20 + 1) * SQUARE_SIZE + BORDER_SIZE + 1));
		setLocation (100, 10);
		setBackground (offWhite);
		setLayout (new BoxLayout (this, BoxLayout.PAGE_AXIS));

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
		JMenuItem newOption = new JMenuItem ("New Game (n)");
		newOption.setActionCommand ("newGame");
		newOption.addActionListener (this);
		JMenuItem levelOption = new JMenuItem ("Restart Level (r)");
		levelOption.setActionCommand ("restartLevel");
		levelOption.addActionListener (this);
		JMenuItem exitOption = new JMenuItem ("Exit (e)");
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

		JMenu AboutMenu = new JMenu ("About");
		AboutMenu.setActionCommand ("about");
		AboutMenu.addActionListener (this);
		//Add Menus to Frame
		JMenuBar mainMenu = new JMenuBar ();
		mainMenu.add (gameMenu);
		mainMenu.add (levelMenu);
		mainMenu.add (SettingsMenu);
		mainMenu.add (AboutMenu);

		frame.setJMenuBar (mainMenu);
		//Images loading
		MediaTracker tracker = new MediaTracker (this);
		hippoImage = Toolkit.getDefaultToolkit ().getImage ("hippo.png");
		tracker.addImage (hippoImage, 0);
		bombImage = Toolkit.getDefaultToolkit ().getImage ("bomb.png");
		tracker.addImage (hippoImage, 1);
		fruitImage = Toolkit.getDefaultToolkit ().getImage ("strawberry.gif");
		tracker.addImage (hippoImage, 1);

		try
		{
			tracker.waitForAll ();
		}
		catch (InterruptedException e)
		{
		}

		frame.setIconImage (hippoImage);
		
		placeBomb();
		placeFruit();
		setFocusable (true); 
		addKeyListener (this);
		addMouseListener (this);

		//Audio Here
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
		backgroundAudio.setFramePosition (0); //<-- play sound file again from beginning
		backgroundAudio.loop(Clip.LOOP_CONTINUOUSLY);
	}
	//----------------------------------------------------------------
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

	public void  placeBomb() {
		for (int i=0; i<5;i++) {
			int x = (int)(Math.random()*(20))+1;
			int y = (int)(Math.random()*(20))+1;
			if (board [x][y]==EMPTY&&x!=11&&y!=1) {
				board [x][y] = BOMB;	
				bombRectArr[i].setLocation(y*30,x*30);
			}
			else
				i--;
		}
	}

	public void placeFruit() {
		boolean unique = false;
		while(unique==false) {
			int x = (int)(Math.random()*(18))+2;
			int y = (int)(Math.random()*(18))+2;
			if(board [x][y]==EMPTY) {
				board [x][y] = FRUIT;
				fruitRect.setLocation(y*30,x*30); 
				
	
				unique=true;
			}
		}
	}
	public void setWalls() {
		//	System.out.print("called");


		for (int wallCount=0; wallCount<coverage; wallCount++) {
			boolean impossible =true;
			while (impossible ==true) {
				int row = (int)(Math.random()*(17)+2);
				int col = (int)(Math.random()*(17)+2);
				//	wallCoordinates[wallCount].x= col*30;
				//	wallCoordinates[wallCount].y= row*30;
				//	System.out.print("Randomized row, col: "+row+", "+col+"   ");

				//check if layout is impossible, criteria:
				// 1. allow no more than one diagonal 
				// 2. max 2 surrounding
				// 3. not allowed on outer two rows/columns
				if (board[row][col]==EMPTY) {	
					int inContact = 0;
					int diagonalInComparison = 0;
					int overallCount = 0;
					for (int rowCount = row-1; rowCount<=row+1; rowCount++) {
						for (int colCount = col-1; colCount<=col+1; colCount++) {
							overallCount++;

							if (rowCount!=row&&colCount!=col) {
								//inContact
								if (board[rowCount][colCount]==WALL) {
									inContact++;
									if (overallCount%2==1)
										diagonalInComparison++;
								}
							}

						}
					}
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
					System.out.println("Wall location: "+wallRectArr[wallCount].x+", "+wallRectArr[wallCount].y+" loop #: "+wallCount+" Success: "+board[row][col]);
				}
			}


		}


	}

	@Override
	public void run() {
		while(gameOver==false) {
			
			//main game loop	
			move();
			if (gameOver==true)
				break;
			this.repaint();	

			try {
				Thread.sleep(sleepDuration);
			} catch(Exception e) {
				e.printStackTrace();
			}

		}

		if (gameOver==true && hitBomb==true) {
			resetBoard();
			hitBomb = false;

			JOptionPane.showMessageDialog (this, "Bomb hit! You Lost!",
					"Game Over", JOptionPane.WARNING_MESSAGE);

			return;
		}
		else if (gameOver==true && pointCount<WIN_MINIMUM) {
			resetBoard();

			JOptionPane.showMessageDialog (this, "Border hit! You Lost!",
					"Game Over", JOptionPane.WARNING_MESSAGE);

			return;
		}
		else if (pointCount== WIN_MINIMUM) {
			gameOver=true;
			if (level<3) {
				level++;
				resetBoard();

			}
			JOptionPane.showMessageDialog (this, "You Won!",
					"Game Over", JOptionPane.WARNING_MESSAGE);

			return;
		}
	}

	public void checkStatus() {

		//Bounds
		if(rect.x < 30 || rect.x > screenWidth - rect.width-SQUARE_SIZE) {
			gameOver=true;
			if(soundOn==true) {
				borderAudio.setFramePosition (30000); //<-- play sound file again from beginning
				borderAudio.start ();
			}
		}
		if(rect.y < 30||rect.y > screenHeight - rect.height-SQUARE_SIZE) {
			if (soundOn==true) {
				borderAudio.setFramePosition (30000); //<-- play sound file again from beginning
				borderAudio.start ();
			}
			gameOver=true;
		}
		//Bomb
	//	if ((rect.x/30.0)%1==0.0&&(rect.y/30.0)%1==0.0) {
	//		if (board[(rect.y)/30][(rect.x)/30]==BOMB) {
	for (int i=0;i<5;i++) {
		if(rect.intersects(bombRectArr[i])){
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
	
		if (soundOn==true) {
					bombAudio.setFramePosition (30000); //<-- play sound file again from beginning
					bombAudio.start ();
				}
				gameOver=true;
				hitBomb = true;
		}
	}
		//Fruit
	//	if ((rect.x/30.0)%1==0&&(rect.y/30.0)%1==0) {
	//		if (board[(rect.y)/30][(rect.x)/30]==FRUIT) 
				if(rect.intersects(fruitRect)){
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
				if (soundOn==true) {
					fruitAudio.setFramePosition (8300); //<-- play sound file again from beginning
					fruitAudio.start ();
				}

				board[(rect.y)/30][(rect.x)/30]=EMPTY;
				if (pointCount==WIN_MINIMUM) {
					gameOver=true;
					return;
				}
				placeFruit();
			}


		
		if (level !=1)	{
			//walls
			for(int i = 0; i < wallRectArr.length; i++) {

				if (rect.intersects(wallRectArr[i])){
					System.out.println("collision");

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

	public void resetBoard() {
		//UPDATE ONCE METHODS ARE MADE
		board = new int [22] [22];
		left = false;
		right = false;
		up = false;
		down = false;
		rect.x=30;
		rect.y=330;
		gameOver = false;
		pointCount=0;
		if (level !=1) {
			initialize();
			setWalls();
		}
		placeBomb();
		placeFruit();

		if (level == 1)
			sleepDuration=9;
		else if (level==2)
			sleepDuration = 7;
		else if (level==3)
			sleepDuration = 5;
		
		thread = new Thread(this);
		thread.start();

		repaint ();

	}

	public void newGame() {
		level = 1;
		resetBoard();
	
	}
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
		//	System.out.println(rect.x+", "+rect.y);

		checkStatus();

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
			level = 1;
			resetBoard();
			sleepDuration = 9 ;
			
		}
		else if (eventName.equals ("level2"))
		{
			level = 2;	
			resetBoard();
			sleepDuration = 7 ;
		}
		else if (eventName.equals ("level3"))
		{
			level = 3;		
			resetBoard();
			sleepDuration = 5 ;

		}
		else if (eventName.equals ("musicMute"))
		{
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
			if (soundState==false)
				soundOn =false;
			else
				soundOn =true;
		}
		else if (eventName.equals ("about"))
		{
			System.exit (0);
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

		// Redraw the board with current pieces
		int wallCount = 0;
		for (int row = 1 ; row <= 20 ; row++)
			for (int column = 1 ; column <= 20 ; column++)
			{
				// Find the x and y positions for each row and column
				int xPos = (column - 1) * SQUARE_SIZE + BORDER_SIZE;
				int yPos = row * SQUARE_SIZE;

				// Draw the squares
				offScreenBuffer.setColor (Color.black);
				offScreenBuffer.drawRect (xPos, yPos, SQUARE_SIZE, SQUARE_SIZE);

				// Draw each piece, depending on the value in board
			//	if (board [row] [column] == BOMB)
			//		offScreenBuffer.drawImage (bombImage, xPos, yPos, SQUARE_SIZE, SQUARE_SIZE, this);
			//	else if (board [row] [column] == FRUIT)
			//		offScreenBuffer.drawImage (fruitImage, xPos, yPos, SQUARE_SIZE, SQUARE_SIZE, this);
				if (board [row] [column] == WALL && wallCount<coverage) {
					offScreenBuffer.setColor(Color.BLACK);
					offScreenBuffer.fillRect(wallRectArr[wallCount].x,wallRectArr[wallCount].y, SQUARE_SIZE, SQUARE_SIZE );
					//		offScreenBuffer.drawImage (fruitImage, xPos, yPos, SQUARE_SIZE, SQUARE_SIZE, this);
					//		System.out.println("Wall #: "+wallCount);
					wallCount++;
				}
			}
		offScreenBuffer.fillRect(fruitRect.x,fruitRect.y,SQUARE_SIZE, SQUARE_SIZE);
		offScreenBuffer.drawImage(fruitImage, fruitRect.x,fruitRect.y,SQUARE_SIZE, SQUARE_SIZE,this );
		
		for (int i =0; i<5;i++) {
			offScreenBuffer.fillRect(bombRectArr[i].x,bombRectArr[i].y,SQUARE_SIZE, SQUARE_SIZE);
			offScreenBuffer.drawImage(bombImage, bombRectArr[i].x,bombRectArr[i].y,SQUARE_SIZE, SQUARE_SIZE,this );

		}
		
		offScreenBuffer.setColor(Color.BLUE);
		offScreenBuffer.fillRect(rect.x,rect.y, SQUARE_SIZE,SQUARE_SIZE);
		offScreenBuffer.drawImage(hippoImage, rect.x,rect.y,SQUARE_SIZE, SQUARE_SIZE,this );

		// Draw next player

		// Transfer the offScreenBuffer to the screen
		g.drawImage (offScreenImage, 0, 0, this);

		

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
		// TODO Auto-generated method stub
		int key = e.getKeyCode();
		if(key == KeyEvent.VK_LEFT) {
			left = true;
			right = false;
			up = false;
			down = false;
		}else if(key == KeyEvent.VK_RIGHT) {
			right = true;
			left = false;
			up = false;
			down = false;
		}if(key == KeyEvent.VK_UP) {
			up = true;
			down = false;
			left = false;
			right = false;
		}else if(key == KeyEvent.VK_DOWN) {
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
		//		int key = e.getKeyCode();
		//		if(key == KeyEvent.VK_LEFT) {
		//			left = false;
		//		}else if(key == KeyEvent.VK_RIGHT) {
		//			right = false;
		//		}else if(key == KeyEvent.VK_UP) {
		//			up = false;
		//		}else if(key == KeyEvent.VK_DOWN) {
		//			down = false;
		//		}
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
		frame = new JFrame ("Hungry Hippo");
		hungryHippo myPanel = new hungryHippo ();
		frame.addKeyListener(myPanel);
		frame.add (myPanel);
		frame.pack ();
		frame.setVisible (true);
		frame.setResizable(false);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
}
