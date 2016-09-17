package wingbots;
import robocode.*;
import java.awt.*;
import robocode.util.*;
import java.awt.geom.*;
import java.awt.Color;

// API help : http://robocode.sourceforge.net/docs/robocode/robocode/Robot.html

/**
 * Yangfan - a robot by Jafar
 */
public class Yangfan extends AdvancedRobot {
	int timesHitWall = 0;
	
	//gun variables
	static double enemyVelocities[][]=new double[400][4];
	static int currentEnemyVelocity;
	static int aimingEnemyVelocity;
	double velocityToAimAt;
	boolean fired;
	double oldTime;
	int count;
	int averageCount;

	//movement variables
	static double direction;	//1 for clockwise or -1 for counterclockwise
	static double turn=2;
	int turnDir=1;
	int moveDir=1;
	double oldEnemyHeading;
	double oldEnergy=100;

	public void run(){
		setColors(Color.blue);
		direction = 1;

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

		// Code directly from simple implementation of wall smoothing here:
		// http://robowiki.net/wiki/Wall_Smoothing/Implementations
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
		if(count==400){
			count=0;
		}

		//calculate our average velocity for our current segment
		averageCount=0;
		velocityToAimAt=0;
		while(averageCount<400){
			velocityToAimAt+=enemyVelocities[averageCount][currentEnemyVelocity];
			averageCount++;
		}
		velocityToAimAt/=400;


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
	
	public void onHitWall(HitWallEvent e) {
		System.out.println("I hit the wall " + timesHitWall);
	}
	
	void setColors(Color color) {
		setBodyColor(color);
		setGunColor(color);
		setRadarColor(color);
		setScanColor(color);
	}
}
