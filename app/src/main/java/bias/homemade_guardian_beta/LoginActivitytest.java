package bias.homemade_guardian_beta;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.kakao.auth.ApiErrorCode;
import com.kakao.auth.ISessionCallback;
import com.kakao.auth.Session;
import com.kakao.network.ErrorResult;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.MeV2ResponseCallback;
import com.kakao.usermgmt.response.MeV2Response;
import com.kakao.util.exception.KakaoException;

import bias.homemade_guardian_beta.common.Util9;
import bias.homemade_guardian_beta.model.UserModel;

public class LoginActivitytest extends AppCompatActivity {
    private SessionCallback sessionCallback;

    //이메일 비밀번호 로그인 모듈 변수
    private FirebaseAuth mAuth=null;
    //현재 로그인 된 유저 정보를 담을 변수
    private FirebaseUser currentUser=null;
    //구글 로그인
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;
    private SignInButton signInButton;
    //카카오 로그인
    String kakaopass = "1234567890";
    int OK =0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logintest);

        mAuth = FirebaseAuth.getInstance();


        //카카오 로그인
        sessionCallback = new SessionCallback(); //세션콜백 초기화
        Session.getCurrentSession().addCallback(sessionCallback);  //현재 세션에 콜백 붙임
        Session.getCurrentSession().checkAndImplicitOpen();  //자동 로그인

        //구글 로그인
        signInButton = findViewById(R.id.signInButton);


        if (currentUser != null) {
            Intent intent = new Intent(getApplication(), MainActivity.class);
            startActivity(intent);
            finish();
        }


        FirebaseAuthgoogle();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(Session.getCurrentSession().handleActivityResult(requestCode, resultCode, data)) {  //카카오 로그인 액티비티에서 넘어온 경우일경우일때 실행
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
            }
        }
    }

    //카카오 로그인
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Session.getCurrentSession().removeCallback(sessionCallback);  //현재 액티비티 제거시 콜백도 같이 제거
    }

    //카카오 로그인
    private class SessionCallback implements ISessionCallback {
        @Override
        public void onSessionOpened() {
            //로그인 세션이 열렸을때
            UserManagement.getInstance().me(new MeV2ResponseCallback() {
                @Override
                public void onFailure(ErrorResult errorResult) {
                    //로그인에 실패했을때.인터넷 연결이 불안정해도 이경우에 해당됨
                    int result = errorResult.getErrorCode();

                    if(result == ApiErrorCode.CLIENT_ERROR_CODE) {
                        Toast.makeText(getApplicationContext(), "네트워크 연결이 불안정합니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(getApplicationContext(),"로그인 도중 오류가 발생했습니다: "+errorResult.getErrorMessage(),Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onSessionClosed(ErrorResult errorResult) {
                    //로그인 도중 세션이 비정상적인 이유로 닫혔을때
                    Toast.makeText(getApplicationContext(),"세션이 닫혔습니다. 다시 시도해 주세요: "+errorResult.getErrorMessage(),Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onSuccess(MeV2Response result) {
                    FirebaseAuthkakaologin(result.getKakaoAccount().getEmail(),kakaopass);
                    FirebaseAuthkakaosignup(result.getKakaoAccount().getEmail(),kakaopass);

                }
            });
        }

        @Override
        public void onSessionOpenFailed(KakaoException e) {
            //로그인 세션이 정상적으로 열리지 않았을 때
            Toast.makeText(getApplicationContext(), "로그인 도중 오류가 발생했습니다. 인터넷 연결을 확인해주세요: "+e.toString(), Toast.LENGTH_SHORT).show();
        }
    }
    //카카오 로그인
    public void FirebaseAuthkakaologin(String email, String password) {
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            //progressDialog.dismiss();
                            if (task.isSuccessful()) {
                                currentUser = mAuth.getCurrentUser();
                                updateUI(currentUser);
                                finish();
                                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                            } else {
                                OK=1;
                                Util9.showMessage(getApplicationContext(), task.getException().getMessage());
                            }
                        }
                    });

    }

    //파이어베이스 카카오 가입 함수
    public void FirebaseAuthkakaosignup(String email, String password){
       if(OK==1){
           final String id = email;
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            if(!task.isSuccessful()) {
                                Util9.showMessage(getApplicationContext(), task.getException().getMessage());
                            }else{

                                //추가
                                currentUser = mAuth.getCurrentUser();
                                final String uid = mAuth.getInstance().getUid();

                                UserModel userModel = new UserModel();
                                userModel.setUid(uid);
                                userModel.setUserid(id);
                                userModel.setUsernm(extractIDFromEmail(id));
                                userModel.setUsermsg("...");

                                FirebaseFirestore db = FirebaseFirestore.getInstance();
                                db.collection("users").document(uid)
                                        .set(userModel)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                updateUI(currentUser);
                                            }
                                        });


                                currentUser = mAuth.getCurrentUser();
                                updateUI(currentUser);
                            }
                        }
                    });


        }

    }

    String extractIDFromEmail(String email){
        String[] parts = email.split("@");
        return parts[0];
    }

    //구글 로그인 메인 함수(onCreate안의 함수)
    public void FirebaseAuthgoogle(){
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                googlesignIn();
            }
        });
    }

    // 구글 회원가입
    private void googlesignIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            //추가
                            currentUser = mAuth.getCurrentUser();
                            final String id = currentUser.getEmail();
                            final String uid = mAuth.getInstance().getUid();

                            UserModel userModel = new UserModel();
                            userModel.setUid(uid);
                            userModel.setUserid(id);
                            userModel.setUsernm(extractIDFromEmail(id));
                            userModel.setUsermsg("...");

                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            db.collection("users").document(uid)
                                    .set(userModel)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            updateUI(currentUser);
                                        }
                                    });


                            currentUser = mAuth.getCurrentUser();
                            updateUI(currentUser);

                        } else {
                            // If sign in fails, display a message to the user.
                            updateUI(null);
                            Util9.showMessage(getApplicationContext(), task.getException().getMessage());
                        }
                    }
                });
    }
    //이동 함수수
    private void updateUI(FirebaseUser user) { //update ui code here
        if (user != null) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

}

