const app = getApp();
const { request } = require('../../utils/request');

Page({
  data: {
    pageTitle: '新增患者档案',
    mode: 'create',
    patientId: null,
    submitting: false,
    genderIndex: 0,
    genderOptions: [
      { label: '男', value: '男' },
      { label: '女', value: '女' }
    ],
    form: {
      patientName: '',
      gender: '男',
      age: '',
      phone: '',
      diseaseType: '',
      idCard: '',
      remark: ''
    }
  },

  onLoad(options) {
    const mode = options.mode === 'edit' ? 'edit' : 'create';
    const patientId = options.id ? Number(options.id) : null;
    this.setData({
      mode,
      patientId,
      pageTitle: mode === 'edit' ? '修改患者档案' : '新增患者档案'
    });
    if (mode === 'edit' && patientId) {
      this.loadPatientDetail(patientId);
    }
  },

  async loadPatientDetail(id) {
    try {
      const result = await request({
        url: `/api/patient/${id}`,
        method: 'GET'
      });
      const detail = result.data || {};
      const genderIndex = this.data.genderOptions.findIndex(item => item.value === detail.gender);
      this.setData({
        genderIndex: genderIndex >= 0 ? genderIndex : 0,
        form: {
          patientName: detail.patientName || '',
          gender: detail.gender || '男',
          age: detail.age === null || detail.age === undefined ? '' : String(detail.age),
          phone: detail.phone || '',
          diseaseType: detail.diseaseType || '',
          idCard: detail.idCard || '',
          remark: detail.remark || ''
        }
      });
    } catch (error) {
      console.error('加载患者档案详情失败', error);
    }
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field;
    const value = e.detail.value;
    this.setData({
      [`form.${field}`]: value
    });
  },

  onGenderChange(e) {
    const index = Number(e.detail.value);
    this.setData({
      genderIndex: index,
      'form.gender': this.data.genderOptions[index].value
    });
  },

  async onSubmit() {
    const { patientName, age, phone, diseaseType } = this.data.form;
    if (!patientName.trim() || !age || !phone.trim() || !diseaseType.trim()) {
      wx.showToast({ title: '请填写完整必填信息', icon: 'none' });
      return;
    }

    this.setData({ submitting: true });
    try {
      const isEdit = this.data.mode === 'edit' && this.data.patientId;
      const result = await request({
        url: isEdit ? `/api/patient/${this.data.patientId}` : '/api/patient/upload',
        method: isEdit ? 'PUT' : 'POST',
        data: this.data.form
      });
      const patientId = result.data || this.data.patientId;
      const currentPatient = {
        id: patientId,
        patientName: this.data.form.patientName.trim(),
        diseaseType: this.data.form.diseaseType.trim(),
        phone: this.data.form.phone.trim()
      };
      wx.setStorageSync('currentPatient', currentPatient);
      app.globalData.currentPatient = currentPatient;
      wx.showToast({ title: isEdit ? '修改成功' : '新增成功', icon: 'success' });
      this.setData({
        genderIndex: 0,
        form: {
          patientName: '',
          gender: '男',
          age: '',
          phone: '',
          diseaseType: '',
          idCard: '',
          remark: ''
        }
      });
      setTimeout(() => {
        wx.navigateBack();
      }, 500);
    } catch (error) {
      console.error('上传患者信息失败', error);
    } finally {
      this.setData({ submitting: false });
    }
  }
});
