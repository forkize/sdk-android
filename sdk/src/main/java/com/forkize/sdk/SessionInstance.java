package com.forkize.sdk;

import java.util.Date;

public class SessionInstance {

    private static SessionInstance instance;

    private String sessionId;

    private long sessionStartTime;
    private long sessionEndTime;
    private long sessionResumeTime;
    private long sessionLength;

  private boolean isDestroyed;
  private boolean isPaused;

    private SessionInstance() {
        this.sessionStartTime = 0;
        this.sessionEndTime = 0;
        this.sessionResumeTime = 0;
        this.sessionLength = 0;
        this.isDestroyed = false;
        this.isPaused = false;
    }

    public static SessionInstance getInstance() {
        if (instance == null) {
            instance = new SessionInstance();
        }
        return instance;
    }

    protected void start(){
        long currentTime = System.currentTimeMillis();
        if(currentTime > this.sessionEndTime || this.isDestroyed ) {
            this.sessionStartTime = currentTime;
            this.sessionResumeTime = currentTime;
            this.sessionEndTime = currentTime + ForkizeConfig.getInstance().getNewSessionInterval();
            this.sessionLength = 0;
            // set isDestroyed to FALSE
            this.isDestroyed = false;
            this.isPaused = false;
            // ** generate session token
            this.sessionId = this.generateSessionId();
            ForkizeEventManager.getInstance().queueSessionStart();
        }
    }

    protected void end(){
        if(!this.isDestroyed) {
            this.isDestroyed = true;
            this.sessionLength += System.currentTimeMillis() - this.sessionResumeTime;
            ForkizeEventManager.getInstance().queueSessionEnd();
        }
    }

    protected void pause(){
        if(!this.isPaused) {
            this.isPaused = true;
            this.sessionLength += System.currentTimeMillis() - this.sessionResumeTime;
        }
    }

    protected void resume(){
        if(this.isPaused) {
            this.isPaused = false;
            this.sessionResumeTime = System.currentTimeMillis();
            if (this.sessionResumeTime > this.sessionEndTime) {
                this.end();
                this.start();
            }
        }
    }

    public String getSessionId() {
        return this.sessionId;
    }

    protected String generateSessionId() {
        // FZ::TODO uuid session
        return "sessionid";
    }

    public long getSessionStartTime() {
        return sessionStartTime;
    }

    public long getSessionLength() {
        return this.sessionLength;
    }
}