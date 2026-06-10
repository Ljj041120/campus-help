<template>
  <view class="page">
    <view v-if="!chatOrderId">
      <view class="title-bar">💬 消息</view>
      <view class="chat-list" v-if="orders.length>0">
        <view v-for="(o,i) in orders" :key="o.id" class="chat-item" @click="openChat(i)">
          <view class="ci-avatar">{{ getTypeEmoji(o.orderType) }}</view>
          <view class="ci-info">
            <text class="ci-name">{{ getChatName(o) }}</text>
            <text class="ci-preview">{{ getChatPreview(o) }}</text>
          </view>
          <text class="ci-arrow">›</text>
        </view>
      </view>
      <view v-else class="empty">暂无订单聊天记录</view>
    </view>
    <view v-else class="chat-window">
      <view class="cw-header">
        <text class="cw-back" @click="chatOrderId=null">‹</text>
        <text class="cw-title">{{ getChatTitle() }} #{{ chatOrderId }}</text>
      </view>
      <scroll-view class="cw-body" scroll-y :scroll-top="scrollTop" scroll-with-animation>
        <view v-for="msg in messages" :key="msg.id" class="msg" :class="msg.senderId===userId?'mine':'other'">
          <text class="msg-text">{{ msg.content }}</text>
          <text class="msg-time">{{ formatTime(msg.createdAt) }}</text>
        </view>
        <view v-if="messages.length===0" class="empty-msg">暂无消息</view>
      </scroll-view>
      <view class="cw-input-area">
        <input v-model="inputText" class="cw-input" placeholder="输入消息..." @confirm="sendMsg" />
        <button class="cw-send" @click="sendMsg" :disabled="!inputText.trim()">发送</button>
      </view>
    </view>
  </view>
</template>

<script>
const api=require('../../api/request.js')
export default{
  data(){return{orders:[],chatOrderId:null,chatOrder:null,messages:[],inputText:'',scrollTop:0,userId:null}},
  onShow(){this.userId=uni.getStorageSync('userId');this.loadOrders()},
  methods:{
    async loadOrders(){try{const r=await api.get('/order/my',{pageNum:1,pageSize:50});if(r&&r.data){const d=r.data;this.orders=d.records||d}}catch(e){}},
    async openChat(i){const o=this.orders[i];if(!o)return;this.chatOrderId=o.id;this.chatOrder=o;this.loadMessages()},
    async loadMessages(){try{const r=await api.get('/chat/messages',{orderId:this.chatOrderId,limit:100});if(r&&r.data){const m=r.data||[];this.messages=m.reverse?m.reverse():m;this.$nextTick(()=>this.scrollTop=99999)}}catch(e){}},
    formatTime(t){return t?t.substring(11,16):''},
    getTypeEmoji(t){const m={1:'📦',2:'🛒',3:'🚀',4:'📌'};return m[t]||'📌'},
    getChatName(o){const m={1:'代取快递',2:'代买物品',3:'代送物品',4:'其他'};return '订单#'+o.id+' - '+(m[o.orderType]||'未知')},
    getChatPreview(o){return o.pickupAddress+' → '+o.deliveryAddress},
    getChatTitle(){const m={1:'代取快递',2:'代买物品',3:'代送物品',4:'其他'};return (m[this.chatOrder?.orderType]||'聊天')},
    async sendMsg(){const t=this.inputText.trim();if(!t)return;try{const targetUserId=this.chatOrder.publisherId===this.userId?this.chatOrder.runnerId:this.chatOrder.publisherId;if(!targetUserId)return uni.showToast({title:'对方未接单',icon:'none'});await api.post('/chat/send',{orderId:this.chatOrderId,receiverId:targetUserId,content:t,messageType:1});this.inputText='';this.loadMessages()}catch(e){uni.showToast({title:'发送失败',icon:'none'})}}
  }
}
</script>

<style scoped>
.page{min-height:100vh;background:#f5f6fa}
.title-bar{padding:15px;font-size:17px;font-weight:700;background:#fff}
.chat-item{display:flex;align-items:center;padding:14px 16px;background:#fff;border-bottom:1px solid #f5f5f5}
.ci-avatar{font-size:30px;margin-right:12px;width:40px;text-align:center}
.ci-info{flex:1}.ci-name{font-size:15px;font-weight:600;color:#333;display:block}.ci-preview{font-size:12px;color:#999;margin-top:2px;display:block}
.ci-arrow{font-size:20px;color:#ccc}
.empty,.empty-msg{text-align:center;padding:60px;color:#999}
.chat-window{display:flex;flex-direction:column;height:100vh;background:#f0f0f0}
.cw-header{display:flex;align-items:center;padding:12px 16px;background:#fff;border-bottom:1px solid #eee}
.cw-back{font-size:24px;color:#667eea;margin-right:12px;padding:5px}
.cw-title{font-size:16px;font-weight:600}
.cw-body{flex:1;padding:12px 16px;overflow-y:auto}
.msg{margin-bottom:12px;max-width:80%}.msg.mine{margin-left:auto}.msg.other{margin-right:auto}
.msg-text{padding:10px 14px;border-radius:14px;font-size:15px;display:inline-block;word-break:break-all}.mine .msg-text{background:#667eea;color:#fff}.other .msg-text{background:#fff;color:#333}
.msg-time{font-size:10px;color:#bbb;display:block;margin-top:2px}.mine .msg-time{text-align:right}
.cw-input-area{display:flex;align-items:center;padding:10px 12px;background:#fff;border-top:1px solid #eee;padding-bottom:20px}
.cw-input{flex:1;height:40px;background:#f0f0f0;border-radius:20px;padding:0 16px;font-size:14px}
.cw-send{width:60px;height:36px;background:#667eea;border-radius:18px;color:#fff;font-size:14px;border:none;margin-left:10px}.cw-send::after{border:none}
</style>