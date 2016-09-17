package wingbots;
 
import robocode.*;
import java.awt.*;
import robocode.util.*;
import java.awt.geom.*;
 
/**
 * Based on the SuperSpinBot AdvancedRobot design with a few stylistic tweaks
 * Added Iterative Wall Smoothing to avoid the one stupid part of the design of this robot (it hits the wall ALOT)
 */
 
 
 
 
 
public class WingSpinBasic2 extends AdvancedRobot {
	//gun variables
	static double enemyVelocities[][]=new double[800][4];
	static int currentEnemyVelocity;
	static int aimingEnemyVelocity;
	double velocityToAimAt;
	boolean fired;
	double oldTime;
	int count;
	int averageCount;
 
	//movement variables
	static double turn=2;
	int turnDir=1;
	int moveDir=1;
	double oldEnemyHeading;
	double oldEnergy=100;
	public void run(){
		
		// Set colors
		setBodyColor(Color.white);
		setGunColor(Color.white);
		setRadarColor(Color.white);
		setScanColor(Color.white);
 
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		do{
			turnRadarRightRadians(Double.POSITIVE_INFINITY);
		}while(true);
	}
	public void onScannedRobot(ScannedRobotEvent e){
		double absBearing=e.getBearingRadians()+getHeadingRadians();
		Graphics2D g=getGraphics();
 
		//increase our turn speed amount each tick,to a maximum of 8 and a minimum of 4
		turn+=0.2*Math.random();
		if(turn>8){
			turn=2;
		}
 
		//when the enemy fires, we randomly change turn direction and whether we go forwards or backwards
		if(oldEnergy-e.getEnergy()<=3&&oldEnergy-e.getEnergy()>=0.1){
			if(Math.random()>.5){
				turnDir*=-1;
			}
			if(Math.random()>.8){
				moveDir*=-1;
			}
		}
 
		//we set our maximum speed to go down as our turn rate goes up so that when we turn slowly, we speed up and vice versa;
		setMaxTurnRate(turn);
		setMaxVelocity(12-turn);

		double direction = turnDir;	//1 for clockwise or -1 for counterclockwise

		// this is the absolute heading I want to move in to go clockwise or
		// counterclockwise around my enemy if I want to move closer to them,
		// I would use less of an offset from absBearing (I'll go right toward
		// them if I move at absBearing)
		double goalDirection = absBearing-Math.PI/2*direction;
		Rectangle2D fieldRect = new Rectangle2D.Double(18, 18, getBattleFieldWidth()-36,
		    getBattleFieldHeight()-36);
		while (!fieldRect.contains(getX()+Math.sin(goalDirection)*120, getY()+
		        Math.cos(goalDirection)*120))
		{
			goalDirection += direction*.1;	//turn a little toward enemy and try again
		}
		double turn =
		    robocode.util.Utils.normalRelativeAngle(goalDirection-getHeadingRadians());
		if (Math.abs(turn) > Math.PI/2)
		{
			turn = robocode.util.Utils.normalRelativeAngle(turn + Math.PI);
			setBack(100);
		}
		else
			setAhead(100);
			setTurnRightRadians(turn);

		//setAhead(90*moveDir);
		//setTurnLeft(90*turnDir);
		



		oldEnergy=e.getEnergy();
 
 
		//find our which velocity segment our enemy is at right now
		if(e.getVelocity()<-2){
			currentEnemyVelocity=0;
		}
		else if(e.getVelocity()>2){
			currentEnemyVelocity=1;
		}
		else if(e.getVelocity()<=2&&e.getVelocity()>=-2){
			if(currentEnemyVelocity==0){
				currentEnemyVelocity=2;
			}
			else if(currentEnemyVelocity==1){
					currentEnemyVelocity=3;
			}
		}
 
		//update the one we are using to determine where to store our velocities if we have fired and there has been enough time for a bullet to reach an enemy
		//(only a rough approximation of bullet travel time);
		if(getTime()-oldTime>e.getDistance()/12.8&&fired==true){
			aimingEnemyVelocity=currentEnemyVelocity;
		}
		else{
			fired=false;
		}
 
		//record a new enemy velocity and raise the count
		enemyVelocities[count][aimingEnemyVelocity]=e.getVelocity();
		count++;
		if(count==800){
			count=0;
		}
 
		//calculate our average velocity for our current segment
		averageCount=0;
		velocityToAimAt=0;
		while(averageCount<800){
			velocityToAimAt+=enemyVelocities[averageCount][currentEnemyVelocity];
			averageCount++;
		}
		velocityToAimAt/=800;
 
 
		//pulled straight out of the circular targeting code on the Robowiki. Note that all I did was replace the enemy velocity and
		//put in pretty graphics that graph the enemies predicted movement(actually the average of their predicted movement) 
		//Press paint on the robot console to see the debugging graphics.
		//Note that this gun can be improved by adding more segments and also averaging turn rate.
		double bulletPower = Math.min(2.4,Math.min(e.getEnergy()/4,getEnergy()/10));
		double myX = getX();
		double myY = getY();
		double enemyX = getX() + e.getDistance() * Math.sin(absBearing);
		double enemyY = getY() + e.getDistance() * Math.cos(absBearing);
		double enemyHeading = e.getHeadingRadians();
		double enemyHeadingChange = enemyHeading - oldEnemyHeading;
		oldEnemyHeading = enemyHeading;
		double deltaTime = 0;
		double battleFieldHeight = getBattleFieldHeight(), 
		       battleFieldWidth = getBattleFieldWidth();
		double predictedX = enemyX, predictedY = enemyY;
		while((++deltaTime) * (20.0 - 3.0 * bulletPower) < 
		      Point2D.Double.distance(myX, myY, predictedX, predictedY)){		
			predictedX += Math.sin(enemyHeading) * velocityToAimAt;
			predictedY += Math.cos(enemyHeading) * velocityToAimAt;
			enemyHeading += enemyHeadingChange;
			g.setColor(Color.red);
			g.fillOval((int)predictedX-2,(int)predictedY-2,4,4);
			if(	predictedX < 18.0 
				|| predictedY < 18.0
				|| predictedX > battleFieldWidth - 18.0
				|| predictedY > battleFieldHeight - 18.0){
 
				predictedX = Math.min(Math.max(18.0, predictedX), 
				    battleFieldWidth - 18.0);	
				predictedY = Math.min(Math.max(18.0, predictedY), 
				    battleFieldHeight - 18.0);
				break;
			}
		}
		double theta = Utils.normalAbsoluteAngle(Math.atan2(
		    predictedX - getX(), predictedY - getY()));
 
		setTurnRadarRightRadians(Utils.normalRelativeAngle(
		    absBearing - getRadarHeadingRadians())*2);
		setTurnGunRightRadians(Utils.normalRelativeAngle(
		    theta - getGunHeadingRadians()));
		if(getGunHeat()==0){
			fire(bulletPower);
			fired=true;
		}
 
 
 
	}

	/*
	 * Bit of flair
	 * Every time you hit an enemy, changes the body and gun color to a random color.
	 */
	public void onBulletHit(BulletHitEvent e) {
		Color randomColor = new Color((float)Math.min(1.0,getEnergy()/100.0), (float)Math.min(1.0,getEnergy()/100.0), (float)Math.min(1.0,getEnergy()/100.0));

 		setBodyColor(randomColor);
		setGunColor(randomColor);
		setRadarColor(Color.black);
		setScanColor(Color.black);
		
	}
	
	public void onBulletMissed(BulletMissedEvent event) {

   	}
	
	public void onBulletHitBullet(BulletHitBulletEvent e){
		
	}
	
	public void onHitWall(HitWallEvent e){
		System.out.println("Oh no, I hit a wall");

	}
 
}