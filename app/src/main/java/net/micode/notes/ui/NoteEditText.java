/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.micode.notes.ui;

import android.content.Context;
import android.graphics.Rect;
import android.text.Layout;
import android.text.Selection;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.widget.EditText;

import net.micode.notes.R;

import java.util.HashMap;
import java.util.Map;
//继承edittext，设置便签设置文本框
public class NoteEditText extends EditText {
    private static final String TAG = "NoteEditText";
    private int mIndex;
    private int mSelectionStartBeforeDelete;

    private static final String SCHEME_TEL = "tel:" ;
    private static final String SCHEME_HTTP = "http:" ;
    private static final String SCHEME_EMAIL = "mailto:" ;
    ///建立一个字符和整数的hash表，用于链接电话，网站，还有邮箱
    private static final Map<String, Integer> sSchemaActionResMap = new HashMap<String, Integer>();
    static {
        sSchemaActionResMap.put(SCHEME_TEL, R.string.note_link_tel);
        sSchemaActionResMap.put(SCHEME_HTTP, R.string.note_link_web);
        sSchemaActionResMap.put(SCHEME_EMAIL, R.string.note_link_email);
    }

    /**
     * Call by the {@link NoteEditActivity} to delete or add edit text
     */
    //在NoteEditActivity中删除或添加文本的操作，可以看做是一个文本是否被变的标记
    public interface OnTextViewChangeListener {
        /**
         * Delete current edit text when {@link KeyEvent#KEYCODE_DEL} happens
         * and the text is null
         */
        //处理删除按键时的操作
        void onEditTextDelete(int index, String text);

        /**
         * Add edit text after current edit text when {@link KeyEvent#KEYCODE_ENTER}
         * happen
         */
        //处理进入按键时的操作
        void onEditTextEnter(int index, String text);

        /**
         * Hide or show item option when text change
         */
        void onTextChange(int index, boolean hasText);
    }

    private OnTextViewChangeListener mOnTextViewChangeListener;
    //根据context设置文本
    public NoteEditText(Context context) {
        super(context, null);
        //用super引用父类变量
        mIndex = 0;
    }
    //设置当前光标
    public void setIndex(int index) {
        mIndex = index;
    }
    //初始化文本修改标记
    public void setOnTextViewChangeListener(OnTextViewChangeListener listener) {
        mOnTextViewChangeListener = listener;
    }
    //AttributeSet 百度了一下是自定义空控件属性，用于维护便签动态变化的属性
    //初始化便签
    public NoteEditText(Context context, AttributeSet attrs) {
        super(context, attrs, android.R.attr.editTextStyle);
    }
    // 根据defstyle自动初始化
    public NoteEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // TODO Auto-generated constructor stub
    }

    @Override
    //view里的函数，处理手机屏幕的所有事件
    /** event：为手机屏幕触摸事件封装类的对象，其中封装了该事件的所有信息*/
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            //重写了需要处理屏幕被按下的事件
            case MotionEvent.ACTION_DOWN:
                //更新当前坐标值
                int x = (int) event.getX();
                int y = (int) event.getY();
                x -= getTotalPaddingLeft();
                y -= getTotalPaddingTop();
                x += getScrollX();
                y += getScrollY();
                //用布局控件layout根据x,y的新值设置新的位置
                Layout layout = getLayout();
                int line = layout.getLineForVertical(y);
                int off = layout.getOffsetForHorizontal(line, x);
                //更新光标新的位置
                Selection.setSelection(getText(), off);
                break;
        }

        return super.onTouchEvent(event);
    }

    @Override
    //处理用户按下一个键盘按键时会触发的事件
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            //根据按键的 Unicode 编码值来处理
            case KeyEvent.KEYCODE_ENTER:
                //“进入”按键
                if (mOnTextViewChangeListener != null) {
                    return false;
                }
                break;
            case KeyEvent.KEYCODE_DEL:
                //“删除”按键
                mSelectionStartBeforeDelete = getSelectionStart();
                break;
            default:
                break;
        }
        //继续执行父类的其他点击事件
        return super.onKeyDown(keyCode, event);
    }

    @Override
    //处理用户松开一个键盘按键时会触发的事件
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch(keyCode) {
        //根据按键的 Unicode 编码值来处理，有删除和进入2种操作
            case KeyEvent.KEYCODE_DEL:
                if (mOnTextViewChangeListener != null) {
                    //若是被修改过
                    if (0 == mSelectionStartBeforeDelete && mIndex != 0) {
                        //若之前有被修改并且文档不为空
                        mOnTextViewChangeListener.onEditTextDelete(mIndex, getText().toString());
                        //利用上文OnTextViewChangeListener对KEYCODE_DEL按键情况的删除函数进行删除
                        return true;
                    }
                } else {
                    Log.d(TAG, "OnTextViewChangeListener was not seted");
                    //其他情况报错，文档的改动监听器并没有建立
                }
                break;
            case KeyEvent.KEYCODE_ENTER:
                //同上也是分为监听器是否建立2种情况
                if (mOnTextViewChangeListener != null) {
                    int selectionStart = getSelectionStart();
                    //获取当前位置
                    String text = getText().subSequence(selectionStart, length()).toString();
                    //获取当前文本
                    setText(getText().subSequence(0, selectionStart));
                    //根据获取的文本设置当前文本
                    mOnTextViewChangeListener.onEditTextEnter(mIndex + 1, text);
                    //当{@link KeyEvent#KEYCODE_ENTER}添加新文本 
                } else {
                    Log.d(TAG, "OnTextViewChangeListener was not seted");
                  //其他情况报错，文档的改动监听器并没有建立
                }
                break;
            default:
                break;
        }
        //继续执行父类的其他按键弹起的事件
        return super.onKeyUp(keyCode, event);
    }

    @Override
    //当焦点发生变化时，会自动调用该方法来处理焦点改变的事件
    /** focused:触发该事件的View是否获得了焦点，当该控件获得焦点时，Focused等于true，否则等于false。
     *  direction:焦点移动的方向，用数值表示
     *  rect:在触发事件的View的坐标系中，前一个获得焦点的矩形区域，如果不可用则为null
     * */
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        if (mOnTextViewChangeListener != null) {
            //若监听器已经建立
            if (!focused && TextUtils.isEmpty(getText())) {
                //获取到焦点并且文本不为空
                mOnTextViewChangeListener.onTextChange(mIndex, false);
                //mOnTextViewChangeListener子函数，置false隐藏事件选项
            } else {
                mOnTextViewChangeListener.onTextChange(mIndex, true);
              //mOnTextViewChangeListener子函数，置true显示事件选项
            }
        }
        //继续执行父类的其他焦点变化的事件
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }

    @Override
    //生成上下文菜单
    protected void onCreateContextMenu(ContextMenu menu) {
        if (getText() instanceof Spanned) {
            //有文本存在
            int selStart = getSelectionStart();
            int selEnd = getSelectionEnd();
            //获取文本开始和结尾位置

            int min = Math.min(selStart, selEnd);
            int max = Math.max(selStart, selEnd);
            //获取开始到结尾的最大值和最小值
            
            final URLSpan[] urls = ((Spanned) getText()).getSpans(min, max, URLSpan.class);
            //设置url的信息的范围值
            if (urls.length == 1) {
                int defaultResId = 0;
                for(String schema: sSchemaActionResMap.keySet()) {
                    //获取计划表中所有的key值
                    if(urls[0].getURL().indexOf(schema) >= 0) {
                        //若url可以添加则在添加后将defaultResId置为key所映射的值
                        defaultResId = sSchemaActionResMap.get(schema);
                        break;
                    }
                }

                if (defaultResId == 0) {
                    //defaultResId == 0则说明url并没有添加任何东西，所以置为连接其他SchemaActionResMap的值
                    defaultResId = R.string.note_link_other;
                }

                //建立菜单
                menu.add(0, 0, 0, defaultResId).setOnMenuItemClickListener(
                        new OnMenuItemClickListener() {
                            //新建按键监听器
                            public boolean onMenuItemClick(MenuItem item) {
                                // goto a new intent
                                urls[0].onClick(NoteEditText.this);
                                //根据相应的文本设置菜单的按键
                                return true;
                            }
                        });
            }
        }
        //继续执行父类的其他菜单创建的事件
        super.onCreateContextMenu(menu);
    }
}
