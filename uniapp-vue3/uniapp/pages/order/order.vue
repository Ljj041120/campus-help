<template>
  <view class="page">
    <!-- 列表 -->
    <view v-if="!detailId">
      <view class="tabs">
        <view class="tab" :class="{active:tab===0}" @click="switchTab(0)">我发布的</view>
        <view class="tab" :class="{active:tab===1}" @click="switchTab(1)">我接的单</view>
      </view>
      <scroll-view class="status-bar" scroll-x>
        <text v-for="s in statusFilters" :key="s.v" class="status-tag" :class="{active:filterStatus===s.v}" @click="filterBy(s.v)">{{s.l}}</text>
      </scroll-view>
      <view class="list" v-if="orders.length>0">
        <view v-for="o in orders" :key="o.id" class="card" @click="openDetail(o.id)">
          <view class="card-top">
            <text class="type-tag" :class="'t'+o.orderType">{{ getTypeName(o.orderType) }}</text>
            <text class="amount">{{'¥'+o.amount}}</text>
          </view>
          <view class="route"><text class="dot pickup">取</text><text class="addr">{{o.pickupAddress}}</text></view>
          <view class="route"><text class="dot delivery">送</text><text class="addr">{{o.deliveryAddress}}</text></view>
          <view class="card-foot">
            <text class="time">{{ formatTime(o.createdAt) }}</text>
            <text class="status" :class="'s'+o.status">{{ getStatusName(o.status) }}</text>
          </view>
        </view>
      </view>
      <view v-else class="empty">暂无订单</view>
    </view>
    <!-- 详情 -->
    <view v-else class="detail">
      <view class="detail-header">
        <text class="back" @click="detailId=null">‹ 返回</text>
        <text class="detail-title">{{'订单 #'+detailId}}</text>
      </view>
      <view class="d-section" v-if="detail">
        <text class="d-type">{{ getTypeName(detail.orderType) }}</text>
        <text class="d-amount">{{'¥'+detail.amount}}</text>
        <text class="d-status" :class="'s'+detail.status">{{ getStatusName(detail.status) }}</text>
      </view>
      <view class="d-section" v-if="detail">
        <view class="d-row"><text class="d-label">取货</text><text>{{detail.pickupAddress}}</text></view>
        <view class="d-row"><text class="d-label">送货</text><text>{{detail.deliveryAddress}}</text></view>
        <view class="d-row"><text class="d-label">时间</text><text>{{detail.createdAt}}</text></view>
        <view class="d-row" v-if="detail.description"><text class="d-label">描述</text><text>{{detail.description}}</text></view>
        <view class="d-row" v-if="detail.remark"><text class="d-label">备注</text><text>{{detail.remark}}</text></view>
      </view>
      <view class="d-section" v-if="detail">
        <view class="d-row"><text class="d-label">服务费</text><text>{{'¥'+(detail.platformFee||0)}}</text></view>
        <view class="d-row"><text class="d-label">跑腿收入</text><text>{{'¥'+(detail.runnerIncome||0)}}</text></view>
      </view>
      <view class="actions" v-if="detail">
        <button v-if="detail.status===0&&isPublisher" class="btn pay" @click="doAction('pay')">去支付</button>
        <button v-if="detail.status===1&&!isPublisher" class="btn accept" @click="doAction('accept')">抢单</button>
        <button v-if="detail.status===2&&!isPublisher" class="btn start" @click="doAction('start')">开始配送</button>
        <button v-if="detail.status===3&&!isPublisher" class="btn done" @click="doAction('deliver')">确认送达</button>
        <button v-if="detail.status===4&&isPublisher" class="btn confirm" @click="doAction('confirm')">确认收货</button>
        <button v-if="(detail.status===0||detail.status===1)&&isPublisher" class="btn cancel" @click="doAction('cancel')">取消订单</button>
        <button v-if="detail.status===5" class="btn rate" @click="doRate()">评价</button>
      </view>
    </view>
  </view>
</template>

<script>
const api=require('../../api/request.js')
const typeMap={1:'代取快递',2:'代买物品',3:'代送物品',4:'其他'}
const statusMap={0:'待支付',1:'待接单',2:'已接单',3:'进行中',4:'待确认',5:'已完成',6:'已取消',7:'申诉中'}
export default{
  data(){return{
    tab:0,filterStatus:null,orders:[],detailId:null,detail:null,
    statusFilters:[{v:null,l:'全部'},{v:1,l:'待接单'},{v:2,l:'已接单'},{v:3,l:'进行中'},{v:4,l:'待确认'},{v:5,l:'已完成'}]
  }},
  computed:{isPublisher(){return this.detail&&uni.getStorageSync('userId')==this.detail.publisherId}},
  onShow(){this.detailId=null;this.loadOrders()},
  methods:{
    getTypeName(t){return typeMap[t]||'未知'},
    getStatusName(s){return statusMap[s]||'未知'},
    formatTime(t){return t?t.substring(0,16):''},
    switchTab(t){this.tab=t;this.loadOrders()},
    filterBy(v){this.filterStatus=v;this.loadOrders()},
    async loadOrders(){
      try{const r=await api.get('/order/my',{pageNum:1,pageSize:50,status:this.filterStatus});if(r&&r.data){const d=r.data;const uid=uni.getStorageSync('userId');this.orders=(d.records||d).filter(o=>this.tab===0?o.publisherId==uid:o.runnerId==uid)}}catch(e){}
    },
    async openDetail(id){this.detailId=id;try{const r=await api.get('/order/'+id);if(r&&r.data)this.detail=r.data}catch(e){}},
    async doAction(act){
      try{
        if(act==='pay')await api.post('/order/'+this.detailId+'/pay?alipayTradeNo=mock_'+Date.now())
        else if(act==='accept')await api.post('/order/accept',{orderId:this.detailId})
        else if(act==='start')await api.put('/order/'+this.detailId+'/status?status=3')
        else if(act==='deliver')await api.put('/order/'+this.detailId+'/status?status=4')
        else if(act==='confirm')await api.put('/order/'+this.detailId+'/status?status=5')
        else if(act==='cancel')await api.put('/order/'+this.detailId+'/status?status=6')
        uni.showToast({title:'操作成功',icon:'success'})
        this.openDetail(this.detailId);this.loadOrders()
      }catch(e){uni.showToast({title:'操作失败',icon:'none'})}
    },
    doRate(){uni.showToast({title:'评价功能开发中',icon:'none'})}
  }
}
</script>

<style scoped>
.page{min-height:100vh;background:#f5f6fa}
.tabs{display:flex;background:#fff}
.tab{flex:1;text-align:center;padding:14px;font-size:15px;color:#666;border-bottom:2px solid transparent}
.tab.active{color:#667eea;border-bottom-color:#667eea;font-weight:600}
.status-bar{white-space:nowrap;padding:10px 15px;background:#fff;border-top:1px solid #f0f0f0}
.status-tag{display:inline-block;padding:5px 14px;margin-right:8px;border-radius:14px;font-size:12px;background:#f0f0f0;color:#666}
.status-tag.active{background:#667eea;color:#fff}
.list{padding:10px 15px}
.card{background:#fff;border-radius:12px;padding:14px;margin-bottom:10px}
.card-top{display:flex;justify-content:space-between;align-items:center;margin-bottom:8px}
.type-tag{font-size:12px;padding:2px 8px;border-radius:6px}.t1{background:#e3f2fd;color:#1976d2}.t2{background:#fff3e0;color:#f57c00}.t3{background:#e8f5e9;color:#388e3c}.t4{background:#f3e5f5;color:#7b1fa2}
.amount{font-size:18px;font-weight:700;color:#f44336}
.route{display:flex;align-items:center;margin-bottom:4px}
.dot{width:18px;height:18px;border-radius:4px;font-size:10px;color:#fff;display:flex;align-items:center;justify-content:center;margin-right:8px;flex-shrink:0}.dot.pickup{background:#4caf50}.dot.delivery{background:#f44336}
.addr{font-size:13px;color:#333;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.card-foot{display:flex;justify-content:space-between;margin-top:8px;font-size:12px;color:#999}
.s1{color:#f59e0b}.s2{color:#3b82f6}.s3{color:#8b5cf6}.s5{color:#10b981}.s6{color:#ef4444}
.empty{text-align:center;padding:60px;color:#999}
.detail{min-height:100vh;background:#f5f6fa}
.detail-header{display:flex;align-items:center;padding:15px;background:#fff;border-bottom:1px solid #eee}
.back{font-size:18px;color:#667eea;padding:5px}
.detail-title{flex:1;text-align:center;font-size:16px;font-weight:600}
.d-section{background:#fff;margin:10px 15px;border-radius:12px;padding:16px}
.d-type{font-size:14px;color:#666}.d-amount{font-size:28px;font-weight:700;color:#f44336;margin:8px 0}.d-status{font-size:14px;padding:4px 12px;border-radius:8px;display:inline-block;background:#e8f5e9;color:#10b981}
.d-row{display:flex;padding:8px 0;border-bottom:1px solid #f5f5f5;border:none;font-size:14px}.d-label{width:60px;color:#999;flex-shrink:0}
.actions{display:flex;flex-wrap:wrap;gap:10px;padding:15px}
.btn{padding:12px 24px;border-radius:10px;font-size:15px;font-weight:600;border:none;color:#fff}.btn::after{border:none}.pay{background:#f44336}.accept{background:#667eea}.start{background:#3b82f6}.done{background:#10b981}.confirm{background:#10b981}.cancel{background:#999}.rate{background:#f59e0b}
</style>