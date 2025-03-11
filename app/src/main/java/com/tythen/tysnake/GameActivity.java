package com.tythen.tysnake;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Shader;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static com.tythen.tysnake.Constant.*;

public class GameActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private String direction = "right";
    private int foodX = 0, foodY = 0;
    private double currentValue = 0;
    private int foodX1 = 0, foodY1 = 0;
    private int foodX2 = 0, foodY2 = 0;
    private int score = 0;
    private int spid = 0;
    private TextView tv_score;
    private List<SnakePoint> snakePoints = new ArrayList();
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Timer timer;
    private Canvas canvas = null;
    private Paint pointColor = null;
    private boolean gameOver;
    private SoundPool soundPool;
    private MediaPlayer mediaPlayer;
    private int eatingSoundId;
    private int deadSoundId;


    private double kp; // 比例增益
    private double ki; // 积分增益
    private double kd; // 微分增益

    private double setpoint; // 目标值
    private double prevError; // 上一次的误差
    private double integral; // 积分项



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        surfaceView = findViewById(R.id.sv_game);
        surfaceView.getHolder().addCallback(this);//注册回调方法
        tv_score = findViewById(R.id.tv_score);

        Button btn_up = findViewById(R.id.btn_up);
        Button btn_right = findViewById(R.id.btn_right);
        Button btn_left = findViewById(R.id.btn_left);
        Button btn_down = findViewById(R.id.btn_down);

        btn_up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!direction.equals("down")) {
                    direction = "up";
                }
            }
        });
        btn_right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!direction.equals("left")) {
                    direction = "right";
                }
            }
        });
        btn_left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!direction.equals("right")) {
                    direction = "left";
                }
            }
        });
        btn_down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!direction.equals("up")) {
                    direction = "down";
                }
            }
        });



        //音乐
        mediaPlayer = MediaPlayer.create(this, R.raw.bgm);
        mediaPlayer.setLooping(true);
        //音效
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setAudioAttributes(audioAttributes)
                .build();

        eatingSoundId = soundPool.load(this, R.raw.eating, 1);
        deadSoundId = soundPool.load(this,R.raw.dead,1);
    }
    @Override
    public void onResume() {
        super.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause(); // 暂停音乐，以便在应用暂停时停止播放
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaPlayer.release(); // 释放MediaPlayer资源
        soundPool.release();
    }
    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        this.surfaceHolder = surfaceHolder;//获取surfaceHolder
        init();//初始化
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {

    }

    //初始化标签和蛇信息
    private void init() {

        snakePoints.clear();
        score = 0;
        tv_score.setText("0");
        direction = "right";
        int startX = 3 * pointSize;
        for (int i = 0; i < defaultTablePoints; i++) {
            SnakePoint snakePoint = new SnakePoint(startX, pointSize);
            snakePoints.add(snakePoint); //添加蛇的点
            startX -= 2 * pointSize;
        }
        PIDController(1.0, 0.1, 0.01, 0);
        addPoint();//添加食物
        addPoint_1();
        addPoint_2();
        moveSnake();//移动蛇
        mediaPlayer.start();
    }

    //实际上是创建了食物
    private void addPoint() {
        int newFoodX = new Random().nextInt((surfaceView.getWidth() - 2 * pointSize) / pointSize);//左右两边各自留一个半径，然后除以半径得到一共有多少个点，然后随机生成一个点
        int newFoodY = new Random().nextInt((surfaceView.getHeight() - 2 * pointSize) / pointSize);
        if (newFoodX % 2 != 0) {
            newFoodX++;//必须为偶数
        }
        if (newFoodY % 2 != 0) {
            newFoodY++;
        }
        foodX = (newFoodX * pointSize) + pointSize;//偶数乘以半径加上半径得到一个点的正中心
        foodY = (newFoodY * pointSize) + pointSize;
    }
    private void addPoint_1() {
        int newFoodX1 = new Random().nextInt((surfaceView.getWidth() - 2 * pointSize) / pointSize);//左右两边各自留一个半径，然后除以半径得到一共有多少个点，然后随机生成一个点
        int newFoodY1 = new Random().nextInt((surfaceView.getHeight() - 2 * pointSize) / pointSize);
        if (newFoodX1 % 2 != 0) {
            newFoodX1++;//必须为偶数
        }
        if (newFoodY1 % 2 != 0) {
            newFoodY1++;
        }
        foodX1 = (newFoodX1 * pointSize) + pointSize;//偶数乘以半径加上半径得到一个点的正中心
        foodY1 = (newFoodY1 * pointSize) + pointSize;
    }


    private void addPoint_2() {
        int newFoodX2 = new Random().nextInt((surfaceView.getWidth() - 2 * pointSize) / pointSize);//左右两边各自留一个半径，然后除以半径得到一共有多少个点，然后随机生成一个点
        int newFoodY2 = new Random().nextInt((surfaceView.getHeight() - 2 * pointSize) / pointSize);
        if (newFoodX2 % 2 != 0) {
            newFoodX2++;//必须为偶数
        }
        if (newFoodY2 % 2 != 0) {
            newFoodY2++;
        }
        if(newFoodY2 > 20){
            newFoodY2 = 57;
        }
        else{
            newFoodY2 = 1;
        }
        if(newFoodX2 > 20 || newFoodY2 == 1){
            newFoodX2 = 45;
        }
        else{
            newFoodX2 = 1;
        }
        if((newFoodX2 * pointSize) + pointSize == foodX2 && (newFoodY2 * pointSize) + pointSize == foodY2){
            newFoodY2 = 58 - foodX2;
        }
        foodX2 = (newFoodX2 * pointSize) + pointSize;//偶数乘以半径加上半径得到一个点的正中心
        foodY2 = (newFoodY2 * pointSize) + pointSize;
    }

    private void drawCartoonSnakeBody(Canvas canvas, int x, int y, int pointSize) {
        Paint bodyPaint = new Paint();
        bodyPaint.setShader(new LinearGradient(x - pointSize, y - pointSize, x + pointSize, y + pointSize,
                Color.parseColor("#FF6F61"), Color.parseColor("#FFD166"), Shader.TileMode.MIRROR));
        bodyPaint.setAntiAlias(true);

        // 绘制圆角矩形作为蛇的身体
        RectF bodyRect = new RectF(x - pointSize, y - pointSize, x + pointSize, y + pointSize);
        canvas.drawRoundRect(bodyRect, 10, 10, bodyPaint);
    }
    private void moveSnake() {
        //创建计时器，设置定时任务
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                //获取蛇头的位置
                int headPositionX = snakePoints.get(0).getPositionX();
                int headPositionY = snakePoints.get(0).getPositionY();

                //检查是否吃到了食物
                if (foodX < headPositionX+size && foodX > headPositionX-size && foodY > headPositionY-size && foodY < headPositionY+size) {
                    growSnake();
                    addPoint();
                    soundPool.play(eatingSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
                }

                int headPositionX1 = snakePoints.get(0).getPositionX();
                int headPositionY1 = snakePoints.get(0).getPositionY();

                //检查是否吃到了食物
                if (foodX1 < headPositionX1+size && foodX1 > headPositionX1-size && foodY1 > headPositionY1-size && foodY1 < headPositionY1+size) {
                    growSnake();
                    addPoint_1();
                    soundPool.play(eatingSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
                }

                int headPositionX2 = snakePoints.get(0).getPositionX();
                int headPositionY2 = snakePoints.get(0).getPositionY();

                //检查是否吃到了食物
                if (foodX2 < headPositionX2+size && foodX2 > headPositionX2-size && foodY2 > headPositionY2-size && foodY2 < headPositionY2+size) {
                    growSnake();
                    growSnake();
                    addPoint_2();
                    soundPool.play(eatingSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
                }

                double dt = 0.02; // 时间步长
                double nums = score / 40.0;
                setpoint = Math.min(nums,30);
                double output = calculate(currentValue, dt);
                currentValue += output * dt; // 模拟系统响应
                spid = 10 * (int)currentValue;
                System.out.println(currentValue);
                //根据方向移动蛇头，仅仅移动了蛇头，后面的点会通过取代前一个点来移动
                switch (direction) {
                    case "right":
                        snakePoints.get(0).setPositionX(headPositionX + speed + spid);
                        snakePoints.get(0).setPositionY(headPositionY);
                        break;
                    case "left":
                        snakePoints.get(0).setPositionX(headPositionX - speed - spid);
                        snakePoints.get(0).setPositionY(headPositionY);
                        break;
                    case "up":
                        snakePoints.get(0).setPositionX(headPositionX);
                        snakePoints.get(0).setPositionY(headPositionY - speed - spid);
                        break;
                    case "down":
                        snakePoints.get(0).setPositionX(headPositionX);
                        snakePoints.get(0).setPositionY(headPositionY + speed + spid);
                        break;
                }

                if (checkGameOver(headPositionX, headPositionY)) {
                    //游戏结束，关闭计时器
                    timer.purge();
                    timer.cancel();
                    //播放失败音效
                    soundPool.play(deadSoundId,1,1,1,0,1);
                    mediaPlayer.pause();
                    //显示结束的对话框
                    AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);
                    builder.setTitle("游戏结束");
                    builder.setCancelable(false);//不能点周围关闭
                    builder.setMessage("您的得分是：" + score);
                    saveScore();
                    builder.setNegativeButton("返回", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //返回主界面
                            startActivity(new Intent(GameActivity.this,MainActivity.class));
                        }
                    });
                    builder.setPositiveButton("重新开始", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //重启游戏
                            init();
                        }
                    });

                    //创建线程来显示对话框
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            builder.show();
                        }
                    });
                    return;
                }
                else
                {
                    //绘制蛇
                    canvas = surfaceHolder.lockCanvas();
                    canvas.drawColor(Color.WHITE, PorterDuff.Mode.CLEAR);//清除画布

                    //绘制新的头部
                    int X = snakePoints.get(0).getPositionX();
                    int Y = snakePoints.get(0).getPositionY();
                    canvas.drawCircle(X, Y, pointSize, createPointColor());
                    drawCartoonSnakeBody(canvas, X, Y,pointSize);

                    switch (direction) {
                        case "right":
                            canvas.drawCircle(X+30, Y-10, 10, createEyeColor());
                            canvas.drawCircle(X+30, Y+10, 10, createEyeColor());
                            canvas.drawCircle(X+32, Y-10, 2, createBlackColor());
                            canvas.drawCircle(X+32, Y+10, 2, createBlackColor());
                            break;
                        case "left":
                            canvas.drawCircle(X-30, Y-10, 10, createEyeColor());
                            canvas.drawCircle(X-30, Y+10, 10, createEyeColor());
                            canvas.drawCircle(X-32, Y-10, 2, createBlackColor());
                            canvas.drawCircle(X-32, Y+10, 2, createBlackColor());
                            break;
                        case "up":
                            canvas.drawCircle(X+10, Y-30, 10, createEyeColor());
                            canvas.drawCircle(X-10, Y-30, 10, createEyeColor());
                            canvas.drawCircle(X+10, Y-32,2, createBlackColor());
                            canvas.drawCircle(X-10, Y-32,  2, createBlackColor());
                            break;
                        case "down":
                            canvas.drawCircle(X+10, Y+30, 10, createEyeColor());
                            canvas.drawCircle(X-10, Y+30, 10, createEyeColor());
                            canvas.drawCircle(X+10, Y+32,2, createBlackColor());
                            canvas.drawCircle(X-10, Y+32, 2, createBlackColor());
                            break;
                    }

                    //绘制食物
                    Paint foodPaint = new Paint();
                    foodPaint.setColor(Color.parseColor("#FF3B3B")); // 红色
                    foodPaint.setAntiAlias(true);
                    canvas.drawCircle(foodX, foodY, 20, foodPaint);

                    // 苹果的叶子
                    Paint leafPaint = new Paint();
                    leafPaint.setColor(Color.parseColor("#4CAF50")); // 绿色
                    leafPaint.setAntiAlias(true);
                    canvas.drawRect(foodX - 10, foodY - 30, foodX + 10, foodY - 20, leafPaint); // 叶子

                    // 苹果的高光
                    Paint highlightPaint = new Paint();
                    highlightPaint.setColor(Color.parseColor("#FFEB3B")); // 黄色
                    highlightPaint.setAntiAlias(true);

                    canvas.drawCircle(foodX + 10, foodY - 10, 5, highlightPaint);




                    // 绘制五角星
                    Paint starPaint = new Paint();
                    starPaint.setColor(Color.parseColor("#FFD700")); // 五角星颜色（金色）
                    starPaint.setAntiAlias(true);
                    starPaint.setStyle(Paint.Style.FILL); // 填充模式

// 五角星的中心坐标和半径
                    float centerX = foodX2; // 五角星中心X坐标
                    float centerY = foodY2; // 五角星中心Y坐标
                    float outerRadius = 40; // 外接圆半径
                    float innerRadius = 20; // 内接圆半径

// 计算五角星的路径
                    Path starPath = new Path();
                    double angle = Math.toRadians(-18); // 初始角度偏移，使五角星正立

                    for (int i = 0; i < 5; i++) {
                        // 外顶点
                        float outerX = (float) (centerX + outerRadius * Math.cos(angle));
                        float outerY = (float) (centerY + outerRadius * Math.sin(angle));
                        if (i == 0) {
                            starPath.moveTo(outerX, outerY); // 移动到第一个点
                        } else {
                            starPath.lineTo(outerX, outerY); // 连接到外顶点
                        }

                        // 内顶点
                        angle += Math.toRadians(36); // 旋转36度
                        float innerX = (float) (centerX + innerRadius * Math.cos(angle));
                        float innerY = (float) (centerY + innerRadius * Math.sin(angle));
                        starPath.lineTo(innerX, innerY); // 连接到内顶点

                        angle += Math.toRadians(36); // 继续旋转36度
                    }

                    starPath.close(); // 闭合路径

// 绘制五角星
                    canvas.drawPath(starPath, starPaint);

// 添加高光效果（可选）
                    Paint highlightPaints = new Paint();
                    highlightPaints.setColor(Color.parseColor("#FFFFFF")); // 高光颜色（白色）
                    highlightPaints.setAntiAlias(true);
                    highlightPaints.setAlpha(128); // 半透明效果

// 在高光位置绘制一个小圆
                    canvas.drawCircle(centerX + 15, centerY - 15, 10, highlightPaints);

                    Paint foodPaint1 = new Paint();
                    foodPaint1.setColor(Color.parseColor("#FF3B3B")); // 红色
                    foodPaint1.setAntiAlias(true);
                    canvas.drawCircle(foodX1, foodY1, 20, foodPaint1);

                    // 苹果的叶子
                    Paint leafPaint1 = new Paint();
                    leafPaint1.setColor(Color.parseColor("#4CAF50")); // 绿色
                    leafPaint1.setAntiAlias(true);
                    canvas.drawRect(foodX1 - 10, foodY1 - 30, foodX1 + 10, foodY1 - 20, leafPaint1); // 叶子

                    // 苹果的高光
                    Paint highlightPaint1 = new Paint();
                    highlightPaint1.setColor(Color.parseColor("#FFEB3B")); // 黄色
                    highlightPaint1.setAntiAlias(true);

                    canvas.drawCircle(foodX1 + 10, foodY1 - 10, 5, highlightPaint1);

                    //绘制蛇的身体，同时每一个点取代前一个点的位置
                    for (int i = 1; i < snakePoints.size(); i++) {
                        // 保存现在位置
                        int tempX = snakePoints.get(i).getPositionX();
                        int tempY = snakePoints.get(i).getPositionY();

                        // 设置新位置为前一个点的位置
                        snakePoints.get(i).setPositionX(headPositionX);
                        snakePoints.get(i).setPositionY(headPositionY);

                        // 按照新位置绘制
                        int indexX = snakePoints.get(i).getPositionX();
                        int indexY = snakePoints.get(i).getPositionY();
                        drawCartoonSnakeBody(canvas, indexX, indexY, pointSize);

                        // 绘制连接部分（平滑过渡）
                        if (tempX != 0) {
                            int midX = (tempX + headPositionX) / 2;
                            int midY = (tempY + indexY) / 2;
                            drawCartoonSnakeBody(canvas, midX, midY, pointSize);
                        }

                        // 为下一个点保存下一个的新位置
                        headPositionX = tempX;
                        headPositionY = tempY;
                    }
                }
                //解锁画布
                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }, snakeMovingSpeed , snakeMovingSpeed);

    }

    private void saveScore() {
        SharedPreferences shared = getSharedPreferences("score", Context.MODE_PRIVATE);
        int total = shared.getInt("total",0);
        ++total;
        SharedPreferences.Editor editor = shared.edit();
        Date date = new Date(System.currentTimeMillis());
        String nowDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);

        editor.putString(String.valueOf(total+"date"),nowDate);
        editor.putInt(String.valueOf(total+"score"),score);
        editor.putInt("total",total);
        editor.apply();
    }
    public void PIDController(double kp, double ki, double kd, double setpoint) {
        this.kp = kp;
        this.ki = ki;
        this.kd = kd;
        this.setpoint = setpoint;
        this.prevError = 0;
        this.integral = 0;
    }

    public double calculate(double currentValue, double dt) {
        // 计算误差
        double error = setpoint - currentValue;

        // 比例项
        double proportional = kp * error;

        // 积分项
        integral += error * dt;
        double integralTerm = ki * integral;

        // 微分项
        double derivative = (error - prevError) / dt;
        double derivativeTerm = kd * derivative;

        // 更新上一次的误差
        prevError = error;

        // 计算输出
        double output = proportional + integralTerm + derivativeTerm;

        return output;
    }

    public void setSetpoint(double setpoint) {
        this.setpoint = setpoint;
        this.integral = 0; // 重置积分项
        this.prevError = 0; // 重置上一次的误差
    }


    private void growSnake() {
        //创建一个新的点并加入，由于绘制的时候是让后一个点取代前一个点的位置，所以新加入的点在绘制前的处理会得到蛇尾的位置
        snakePoints.add(new SnakePoint(0, 0));
        //创建线程使得分数增加，因为不能在子线程中更新UI，所以要创建线程
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                score++;
                tv_score.setText(String.valueOf(score));
            }
        });
    }

    private boolean checkGameOver(int headPositionX, int headPositionY) {
        boolean gameOver = false;
        if (snakePoints.get(0).getPositionX() < 0 ||
                snakePoints.get(0).getPositionX() > surfaceView.getWidth() ||
                snakePoints.get(0).getPositionY() < 0 ||
                snakePoints.get(0).getPositionY() > surfaceView.getHeight()) {
            gameOver = true;
        }
            for (int i = 1; i < snakePoints.size(); i++) {
                if (snakePoints.get(i).getPositionX() == headPositionX && snakePoints.get(i).getPositionY() == headPositionY) {
                    gameOver = true;
                }
            }

        return gameOver;
    }

    @SuppressLint("ResourceAsColor")
    private Paint createPointColor() {
        if (pointColor == null) {
            pointColor = new Paint();
            pointColor.setColor(Color.GREEN);
            pointColor.setStyle(Paint.Style.FILL);
            pointColor.setAntiAlias(true);
        }
        pointColor.setColor(Color.GREEN);
        return pointColor;
    }

    @SuppressLint("ResourceAsColor")
    private Paint createFoodColor() {
        if (pointColor == null) {
            pointColor = new Paint();
            pointColor.setColor(Color.BLUE);
            pointColor.setStyle(Paint.Style.FILL);
            pointColor.setAntiAlias(true);
        }
        pointColor.setColor(Color.RED);
        return pointColor;
    }
    @SuppressLint("ResourceAsColor")
    private Paint createEyeColor() {
        if (pointColor == null) {
            pointColor = new Paint();
            pointColor.setColor(Color.BLUE);
            pointColor.setStyle(Paint.Style.FILL);
            pointColor.setAntiAlias(true);
        }
        pointColor.setColor(Color.YELLOW);
        return pointColor;
    }
    @SuppressLint("ResourceAsColor")
    private Paint createBlackColor() {
        if (pointColor == null) {
            pointColor = new Paint();
            pointColor.setColor(Color.BLACK);
            pointColor.setStyle(Paint.Style.FILL);
            pointColor.setAntiAlias(true);
        }
        pointColor.setColor(Color.BLACK);
        return pointColor;
    }


}