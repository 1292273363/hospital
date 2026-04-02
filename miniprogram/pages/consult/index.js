const app = getApp();
const { request } = require('../../utils/request');

Page({
  data: {
    currentPatient: null,
    visitOptions: [],
    visitIndex: 0,
    reportLoaded: false,
    report: {},
    historyList: [],
    scarImageUrl: '',
    message: '',
    submitting: false
  },

  onLoad() {
    this.reloadByCurrentPatient();
  },

  onShow() {
    const storedPatient = wx.getStorageSync('currentPatient') || null;
    const currentId = this.data.currentPatient ? this.data.currentPatient.id : null;
    const storedId = storedPatient ? storedPatient.id : null;
    // 仅在当前患者变化时重置，避免选择图片返回后把留言区关闭
    if (currentId !== storedId) {
      this.reloadByCurrentPatient();
    }
  },

  reloadByCurrentPatient() {
    const currentPatient = wx.getStorageSync('currentPatient') || null;
    this.setData({
      currentPatient,
      visitOptions: [],
      visitIndex: 0,
      reportLoaded: false,
      report: {},
      historyList: []
    });
    if (!currentPatient) {
      return;
    }
    this.loadEligibleVisits();
  },

  getCurrentPatient() {
    return this.data.currentPatient;
  },

  async loadEligibleVisits() {
    const patient = this.getCurrentPatient();
    if (!patient) return;
    try {
      const result = await request({
        url: `/api/consult/eligible-visits?patientRecordId=${patient.id}`,
        method: 'GET'
      });
      const visitOptions = (result.data || []).map(item => ({
        id: item.id,
        label: `${item.visitTime} ${item.doctorName}（${item.doctorLevel}）`
      }));
      this.setData({
        visitOptions,
        visitIndex: 0,
        reportLoaded: false,
        report: {}
      });
      if (!visitOptions.length) {
        wx.showToast({ title: '该患者暂无专家看诊记录', icon: 'none' });
      } else {
        this.loadHistory();
      }
    } catch (error) {
      console.error('加载看诊记录失败', error);
    }
  },

  onVisitChange(e) {
    this.setData({
      visitIndex: Number(e.detail.value),
      reportLoaded: false,
      report: {}
    });
  },

  async loadHistory() {
    const patient = this.getCurrentPatient();
    if (!patient) return;
    try {
      const result = await request({
        url: `/api/consult/history?patientRecordId=${patient.id}`,
        method: 'GET',
        showLoading: false
      });
      const historyList = (result.data || []).map(item => ({
        ...item,
        scarImageUrl: item.scarImageUrl && item.scarImageUrl.startsWith('/')
          ? `${app.globalData.baseUrl}${item.scarImageUrl}`
          : item.scarImageUrl
      }));
      this.setData({ historyList });
    } catch (error) {
      console.error('加载在线问诊历史失败', error);
    }
  },

  async loadReport() {
    const current = this.data.visitOptions[this.data.visitIndex];
    if (!this.data.currentPatient) {
      wx.showToast({ title: '请先到中心选择患者档案', icon: 'none' });
      return;
    }
    if (!current) {
      wx.showToast({ title: '请先选择看诊记录', icon: 'none' });
      return;
    }
    try {
      const result = await request({
        url: `/api/consult/report/${current.id}`,
        method: 'GET'
      });
      this.setData({
        reportLoaded: true,
        report: result.data || {}
      });
    } catch (error) {
      console.error('加载诊断报告失败', error);
    }
  },

  chooseImage() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        this.setData({ scarImageUrl: res.tempFiles[0].tempFilePath });
      }
    });
  },

  onMessageInput(e) {
    this.setData({ message: e.detail.value });
  },

  goPatientSelect() {
    wx.switchTab({ url: '/pages/center/index' });
  },

  submitConsult() {
    const current = this.data.visitOptions[this.data.visitIndex];
    if (!current) {
      wx.showToast({ title: '请先选择看诊记录', icon: 'none' });
      return;
    }
    if (!this.data.reportLoaded) {
      wx.showToast({ title: '请先查看诊断报告', icon: 'none' });
      return;
    }
    if (!this.data.scarImageUrl) {
      wx.showToast({ title: '请先上传疤痕图片', icon: 'none' });
      return;
    }
    this.setData({ submitting: true });
    wx.uploadFile({
      url: `${app.globalData.baseUrl}/api/consult/submit`,
      filePath: this.data.scarImageUrl,
      name: 'file',
      formData: {
        visitId: String(current.id),
        message: this.data.message || ''
      },
      header: {
        Authorization: `Bearer ${wx.getStorageSync('token')}`
      },
      success: (res) => {
        let result = {};
        try {
          result = JSON.parse(res.data || '{}');
        } catch (e) {
          wx.showToast({ title: '返回数据解析失败', icon: 'none' });
          return;
        }
        if (result.code !== 200) {
          wx.showToast({ title: result.message || '提交失败', icon: 'none' });
          return;
        }
        wx.showToast({ title: '提交成功', icon: 'success' });
        this.setData({
          scarImageUrl: '',
          message: ''
        });
        this.loadHistory();
      },
      fail: () => {
        wx.showToast({ title: '提交失败，请重试', icon: 'none' });
      },
      complete: () => {
        this.setData({ submitting: false });
      }
    });
  }
});
