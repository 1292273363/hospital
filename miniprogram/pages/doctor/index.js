const app = getApp();

Page({
  data: {
    userInfo: {},
    displayName: '医生',
    greeting: '您好',
    pendingTotal: 5,
    dateStr: '',
    weekdayStr: '',
    pendingList: [
      {
        id: 1,
        name: '李女士',
        avatarText: '李',
        tag: '待复诊',
        tagClass: 'tag-warning',
        desc: '隆鼻手术后复查 · 预约时间: 10:00'
      },
      {
        id: 2,
        name: '王先生',
        avatarText: '王',
        tag: '待诊断',
        tagClass: 'tag-primary',
        desc: '面部脂肪填充 · 预约时间: 10:30'
      },
      {
        id: 3,
        name: '赵先生',
        avatarText: '赵',
        tag: '新咨询',
        tagClass: 'tag-error',
        desc: '双眼皮咨询 · 预约时间: 14:00'
      }
    ],
    weekStats: [
      { label: '新增患者', value: '18' },
      { label: '完成诊疗', value: '32' },
      { label: '预约手术', value: '26' }
    ]
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().refresh();
    }
    const userInfo = wx.getStorageSync('userInfo') || {};
    const name = userInfo.nickName || userInfo.doctorName || '医生';
    const displayName = /医生$/.test(name) ? name : `${name}医生`;
    this.setData({ userInfo, displayName });
    this.updateDateTime();
    this.updateGreeting();
  },

  updateGreeting() {
    const h = new Date().getHours();
    let greeting = '您好';
    if (h >= 5 && h < 12) {
      greeting = '上午好';
    } else if (h >= 12 && h < 18) {
      greeting = '下午好';
    } else {
      greeting = '晚上好';
    }
    this.setData({ greeting });
  },

  updateDateTime() {
    const d = new Date();
    const y = d.getFullYear();
    const m = d.getMonth() + 1;
    const day = d.getDate();
    const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'];
    this.setData({
      dateStr: `${y}年${m}月${day}日`,
      weekdayStr: weekdays[d.getDay()]
    });
  },

  onViewAll() {
    wx.navigateTo({ url: '/pages/doctor/patients' });
  },

  onPatientTap(e) {
    wx.navigateTo({ url: '/pages/doctor/chat' });
  },

  onQuick(e) {
    const action = e.currentTarget.dataset.action;
    switch (action) {
      case 'chat':
        wx.navigateTo({ url: '/pages/doctor/chat' });
        break;
      case 'patients':
        wx.navigateTo({ url: '/pages/doctor/patients' });
        break;
      case 'diagnosis':
      case 'notify':
        wx.showToast({ title: '功能开发中', icon: 'none' });
        break;
      default:
        break;
    }
  }
});
