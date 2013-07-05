package android.tether.dtn.sensor;

import java.util.LinkedList;

import android.util.Log;

public class FetchShakeAlgorithm {
	public static final String MSG_TAG = "DTN->SENSOR->FETCH_SHAKE_ACCELERATER";
	private static final int CACHE_SIZE = 10;
	private static final int JUDGE_VALUE = 5;
	private LinkedList<Double> pastAcceleraterValuesX;
	private LinkedList<Double> pastAcceleraterValuesY;
	private LinkedList<Double> pastAcceleraterValuesZ;
	private LinkedList<Double> nowAcceleraterValuesX;
	private LinkedList<Double> nowAcceleraterValuesY;
	private LinkedList<Double> nowAcceleraterValuesZ;
	
	private LinkedList<Double> pastAcceleraterValues;
	private LinkedList<Double> nowAcceleraterValues;

	
	private ShookBehaverInterface shookBehaverImplements;
	
	public FetchShakeAlgorithm(ShookBehaverInterface impl){
		this.shookBehaverImplements = impl;
		this.pastAcceleraterValuesX = new LinkedList<Double>();
		this.pastAcceleraterValuesY = new LinkedList<Double>();
		this.pastAcceleraterValuesZ = new LinkedList<Double>();
		this.nowAcceleraterValuesX = new LinkedList<Double>();
		this.nowAcceleraterValuesY = new LinkedList<Double>();
		this.nowAcceleraterValuesZ = new LinkedList<Double>();
		this.nowAcceleraterValues = new LinkedList<Double>();
		this.pastAcceleraterValues = new LinkedList<Double>();

	}
	
	public void updateValue(double x,double y,double z){
		update(Math.abs(x)+Math.abs(y)+Math.abs(z));
		if(is_shaken()){
			Log.d(MSG_TAG,"shakeされました");
			this.shookBehaverImplements.after_shook_behaver();
			init();
		}
	}
	
	private void init(){
		this.nowAcceleraterValues = new LinkedList<Double>();
		for(int i=0;i<CACHE_SIZE;i++){
			this.pastAcceleraterValues.addLast((double) 100);
		}
	}
	
	private void update(Double sumXYZ){
		if(this.nowAcceleraterValues.size() > CACHE_SIZE){
			this.nowAcceleraterValues.addLast(sumXYZ);
			Double old = this.nowAcceleraterValues.removeFirst();
			if(this.pastAcceleraterValues.size() > CACHE_SIZE){
				this.pastAcceleraterValues.addLast(old);
				this.pastAcceleraterValues.getFirst();
				this.pastAcceleraterValues.removeFirst();
			} else{
				this.pastAcceleraterValues.addLast(old);
			}
		} else {
			this.nowAcceleraterValues.addLast(sumXYZ);
		}
	}
	
	private void updateX(Double x){
		if(this.nowAcceleraterValuesX.size() > CACHE_SIZE){
			this.nowAcceleraterValuesX.addLast(x);
			Double old = this.nowAcceleraterValuesX.removeFirst();
			if(this.pastAcceleraterValuesX.size() > CACHE_SIZE){
				this.pastAcceleraterValuesX.addLast(old);
				this.pastAcceleraterValuesX.getFirst();
				this.pastAcceleraterValuesX.removeFirst();
			} else{
				this.pastAcceleraterValuesX.addLast(old);
			}
		} else {
			this.nowAcceleraterValuesX.addLast(x);
		}
	}
	
	private void updateY(Double y){
		if(this.nowAcceleraterValuesY.size() > CACHE_SIZE){
			this.nowAcceleraterValuesY.addLast(y);
			Double old = this.nowAcceleraterValuesY.removeFirst();
			if(this.pastAcceleraterValuesY.size() > CACHE_SIZE){
				this.pastAcceleraterValuesY.addLast(old);
				this.pastAcceleraterValuesY.removeFirst();
			} else{
				this.pastAcceleraterValuesY.addLast(old);
			}
		} else {
			this.nowAcceleraterValuesY.addLast(y);
		}
	}
	
	private void updateZ(Double z){
		if(this.nowAcceleraterValuesZ.size() > CACHE_SIZE){
			this.nowAcceleraterValuesZ.addLast(z);
			Double old = this.nowAcceleraterValuesZ.removeFirst();
			if(this.pastAcceleraterValuesZ.size() > CACHE_SIZE){
				this.pastAcceleraterValuesZ.addLast(old);
				this.pastAcceleraterValuesZ.removeFirst();
			} else{
				this.pastAcceleraterValuesZ.addLast(old);
			}
		} else {
			this.nowAcceleraterValuesZ.addLast(z);
		}
	}
	
	private boolean is_shaken(){
		double pastSum = 0;
		double nowSum = 0;
		for(int i=0;i<this.nowAcceleraterValues.size();i++){
			nowSum += this.nowAcceleraterValues.get(i);
		}
		for(int i=0;i<this.pastAcceleraterValues.size();i++){
			pastSum += this.pastAcceleraterValues.get(i);
		}
		if(this.nowAcceleraterValues.size() < 0 || this.pastAcceleraterValues.size() < 0){
			return false;
		}
		double a = nowSum/this.nowAcceleraterValues.size()-pastSum/this.pastAcceleraterValues.size();
		//Log.d(MSG_TAG,"sum:"+a);
		return a > JUDGE_VALUE;
	}
	
	private boolean is_shakenX(){
		double pastSum = 0;
		double nowSum = 0;
		for(int i=0;i<this.nowAcceleraterValuesX.size();i++){
			nowSum += this.nowAcceleraterValuesX.get(i);
		}
		for(int i=0;i<this.pastAcceleraterValuesX.size();i++){
			pastSum += this.pastAcceleraterValuesX.get(i);
		}
		if(this.nowAcceleraterValuesX.size() < 0 || this.pastAcceleraterValuesX.size() < 0){
			return false;
		}
		return Math.abs(nowSum/this.nowAcceleraterValuesX.size()-pastSum/this.pastAcceleraterValuesX.size()) > JUDGE_VALUE;
 	}
	private boolean is_shakenY(){
		double pastSum = 0;
		double nowSum = 0;
		for(int i=0;i<this.nowAcceleraterValuesY.size();i++){
			nowSum += this.nowAcceleraterValuesY.get(i);
		}
		for(int i=0;i<this.pastAcceleraterValuesY.size();i++){
			pastSum += this.pastAcceleraterValuesY.get(i);
		}
		if(this.nowAcceleraterValuesY.size() < 0 || this.pastAcceleraterValuesY.size() < 0){
			return false;
		}
		return Math.abs(nowSum/this.nowAcceleraterValuesY.size()-pastSum/this.pastAcceleraterValuesY.size()) > JUDGE_VALUE;
	}
	private boolean is_shakenZ(){
		double pastSum = 0;
		double nowSum = 0;
		for(int i=0;i<this.nowAcceleraterValuesZ.size();i++){
			nowSum += this.nowAcceleraterValuesZ.get(i);
		}
		for(int i=0;i<this.pastAcceleraterValuesZ.size();i++){
			pastSum += this.pastAcceleraterValuesZ.get(i);
		}
		if(this.nowAcceleraterValuesZ.size() < 0 || this.pastAcceleraterValuesZ.size() < 0){
			return false;
		}
		return Math.abs(nowSum/this.nowAcceleraterValuesZ.size()-pastSum/this.pastAcceleraterValuesZ.size()) > JUDGE_VALUE;


	}
}
